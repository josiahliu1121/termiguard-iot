import os
import glob
import time
import json
import numpy as np
import soundfile as sf
import librosa
from sklearn.model_selection import train_test_split, StratifiedKFold
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier, HistGradientBoostingClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import joblib

TARGET_SR = 16000
SLICE_SEC = 1.0
SLICE_SAMPLES = int(TARGET_SR * SLICE_SEC)

CLASS_MAP = {
    "negative": 0,
    "suspected": 1,
    "positive": 2
}
REV_CLASS_MAP = {v: k for k, v in CLASS_MAP.items()}

def find_class_dirs(base_dir):
    pos_dir = os.path.join(base_dir, "positvie")
    if not os.path.exists(pos_dir):
        pos_dir = os.path.join(base_dir, "positive")
    susp_dir = os.path.join(base_dir, "suspected")
    neg_dir = os.path.join(base_dir, "negative")
    return {
        "positive": pos_dir,
        "suspected": susp_dir,
        "negative": neg_dir
    }

def extract_features_from_slice(y, sr):
    """
    Extracts 13 MFCC features and an acoustic energy score matching the ESP32 packet contract.
    """
    # 13 MFCCs
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
    mfcc_mean = np.mean(mfcc, axis=1) # Shape (13,)
    
    # RMS energy & acoustic score (0 - 100)
    rms = np.sqrt(np.mean(y ** 2))
    # Normalized acoustic score (log/linear scaled to 0-100)
    acoustic_score = min(100.0, float(rms * 800.0))
    
    # Spectral Centroid & Zero Crossing Rate
    sc = np.mean(librosa.feature.spectral_centroid(y=y, sr=sr))
    zcr = np.mean(librosa.feature.zero_crossing_rate(y=y))
    
    # Combined feature vector: 13 MFCCs + acoustic_score + sc + zcr (16 features total)
    # The first 13 are directly the ESP32 MFCCs
    feat = np.hstack([mfcc_mean, [acoustic_score, sc, zcr]])
    return feat

def process_audio_file(file_path, stride_sec=1.0, augment=False):
    """
    Loads an audio file, resamples to TARGET_SR, slices into SLICE_SEC windows with stride_sec,
    and extracts feature vectors.
    """
    features_list = []
    try:
        y, sr = librosa.load(file_path, sr=TARGET_SR, mono=True)
    except Exception as e:
        return features_list
        
    total_len = len(y)
    stride_samples = int(TARGET_SR * stride_sec)
    
    if total_len < SLICE_SAMPLES:
        # Pad short audio
        padded = np.zeros(SLICE_SAMPLES, dtype=np.float32)
        padded[:total_len] = y
        feat = extract_features_from_slice(padded, TARGET_SR)
        features_list.append(feat)
        return features_list

    # Slice into windows
    for start in range(0, total_len - SLICE_SAMPLES + 1, stride_samples):
        slice_y = y[start:start + SLICE_SAMPLES]
        feat = extract_features_from_slice(slice_y, TARGET_SR)
        features_list.append(feat)
        
        # Audio augmentation for positive class to balance dataset
        if augment:
            # 1. Gain scale (0.8x)
            feat_gain_low = extract_features_from_slice(slice_y * 0.8, TARGET_SR)
            features_list.append(feat_gain_low)
            
            # 2. Gain scale (1.25x)
            feat_gain_high = extract_features_from_slice(slice_y * 1.25, TARGET_SR)
            features_list.append(feat_gain_high)
            
            # 3. Add subtle Gaussian noise
            noise = np.random.normal(0, 0.005, len(slice_y))
            feat_noise = extract_features_from_slice(slice_y + noise, TARGET_SR)
            features_list.append(feat_noise)
            
    return features_list

def extract_all_dataset(training_material_dir, cache_path=None):
    if cache_path and os.path.exists(cache_path):
        print(f"[CACHE] Loading cached features from {cache_path}...")
        data = np.load(cache_path)
        return data["X"], data["y"], data["feature_names"]

    class_dirs = find_class_dirs(training_material_dir)
    X_list = []
    y_list = []
    
    print("=== EXTRACTING BIOACOUSTIC FEATURES ===")
    
    # 1. POSITIVE (dense stride 0.25s + augmentation to address class imbalance)
    pos_files = glob.glob(os.path.join(class_dirs["positive"], "*.wav"))
    print(f"-> Processing POSITIVE ({len(pos_files)} files, dense stride 0.25s + 3x augmentation)...")
    for f in pos_files:
        feats = process_audio_file(f, stride_sec=0.25, augment=True)
        for ft in feats:
            X_list.append(ft)
            y_list.append(CLASS_MAP["positive"])
    print(f"   Generated {len(y_list)} positive feature vectors.")
    pos_count = len(y_list)
    
    # 2. SUSPECTED (stride 1.0s, cap around ~1200 samples)
    susp_files = glob.glob(os.path.join(class_dirs["suspected"], "*.wav"))
    print(f"-> Processing SUSPECTED ({len(susp_files)} files, stride 1.0s)...")
    susp_count_start = len(y_list)
    for f in susp_files:
        feats = process_audio_file(f, stride_sec=1.0, augment=False)
        for ft in feats:
            X_list.append(ft)
            y_list.append(CLASS_MAP["suspected"])
        if len(y_list) - susp_count_start >= pos_count * 1.2:
            # Maintain balanced distribution
            break
    print(f"   Generated {len(y_list) - susp_count_start} suspected feature vectors.")
    
    # 3. NEGATIVE (stride 1.0s, cap around ~1200 samples)
    neg_files = glob.glob(os.path.join(class_dirs["negative"], "*.wav"))
    print(f"-> Processing NEGATIVE ({len(neg_files)} files, stride 1.0s)...")
    neg_count_start = len(y_list)
    for f in neg_files:
        feats = process_audio_file(f, stride_sec=1.0, augment=False)
        for ft in feats:
            X_list.append(ft)
            y_list.append(CLASS_MAP["negative"])
        if len(y_list) - neg_count_start >= pos_count * 1.2:
            break
    print(f"   Generated {len(y_list) - neg_count_start} negative feature vectors.")
    
    X = np.array(X_list, dtype=np.float32)
    y = np.array(y_list, dtype=np.int32)
    
    feature_names = [f"mfcc_{i}" for i in range(13)] + ["acoustic_score", "spectral_centroid", "zero_crossing_rate"]
    
    if cache_path:
        os.makedirs(os.path.dirname(cache_path), exist_ok=True)
        np.savez_compressed(cache_path, X=X, y=y, feature_names=feature_names)
        print(f"[CACHE] Saved {len(X)} feature vectors to {cache_path}")
        
    return X, y, feature_names

def train_and_evaluate(training_material_dir, model_save_path, metadata_save_path, cache_path=None):
    start_time = time.time()
    X, y, feature_names = extract_all_dataset(training_material_dir, cache_path)
    
    print(f"\n=== DATASET SUMMARY ===")
    print(f"Total feature vectors: {len(X)}")
    for name, c_id in CLASS_MAP.items():
        print(f"  Class '{name}' (ID {c_id}): {np.sum(y == c_id)} samples ({np.mean(y == c_id)*100:.1f}%)")
        
    # Split train/test
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    print("\n=== TRAINING RANDOM FOREST CLASSIFIER ===")
    model = RandomForestClassifier(
        n_estimators=150,
        max_depth=14,
        min_samples_split=4,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train_scaled, y_train)
    
    # Evaluation
    y_pred = model.predict(X_test_scaled)
    acc = accuracy_score(y_test, y_pred)
    target_names = ["negative", "suspected", "positive"]
    report = classification_report(y_test, y_pred, target_names=target_names, output_dict=True)
    cm = confusion_matrix(y_test, y_pred).tolist()
    
    print(f"\nOverall Test Accuracy: {acc * 100:.2f}%")
    print(classification_report(y_test, y_pred, target_names=target_names))
    print("Confusion Matrix (rows: true, cols: pred):")
    for r in cm:
        print(" ", r)
        
    # Save model artifact bundle
    bundle = {
        "model": model,
        "scaler": scaler,
        "feature_names": feature_names,
        "class_map": CLASS_MAP,
        "rev_class_map": REV_CLASS_MAP,
        "trained_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "metrics": {
            "accuracy": acc,
            "report": report,
            "confusion_matrix": cm,
            "sample_count": len(X)
        }
    }
    
    os.makedirs(os.path.dirname(model_save_path), exist_ok=True)
    joblib.dump(bundle, model_save_path)
    print(f"\n[SAVE] Model bundle saved to: {model_save_path}")
    
    # Save readable metadata
    meta = {
        "model_file": os.path.basename(model_save_path),
        "trained_at": bundle["trained_at"],
        "sample_count": len(X),
        "accuracy": acc,
        "classification_report": report,
        "confusion_matrix": cm,
        "feature_names": feature_names,
        "classes": target_names
    }
    with open(metadata_save_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
    print(f"[SAVE] Metadata saved to: {metadata_save_path}")
    print(f"Elapsed time: {time.time() - start_time:.2f} seconds.")
    return bundle

if __name__ == "__main__":
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "training_material"))
    model_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "models", "termite_classifier_v1.joblib"))
    meta_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "models", "metadata.json"))
    cache_file = os.path.abspath(os.path.join(os.path.dirname(__file__), "data", "extracted_features.npz"))
    
    train_and_evaluate(base_dir, model_path, meta_path, cache_file)
