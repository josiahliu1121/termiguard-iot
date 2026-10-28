import os
import time
import json
import numpy as np
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import joblib

app = FastAPI(
    title="TermaGuard.AI Machine Learning Microservice",
    description="Bioacoustic Termite Classification & Dynamic Retraining Microservice",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "models"))
DEFAULT_MODEL_PATH = os.path.join(MODEL_DIR, "termite_classifier_v1.joblib")
METADATA_PATH = os.path.join(MODEL_DIR, "metadata.json")

class ModelManager:
    def __init__(self):
        self.bundle: Optional[Dict[str, Any]] = None
        self.model = None
        self.scaler = None
        self.feature_names = []
        self.class_map = {}
        self.rev_class_map = {}
        self.metrics = {}
        self.version = "termite_classifier_v1"
        self.is_loaded = False
        self.load_model()

    def load_model(self, model_path: Optional[str] = None):
        target_path = model_path or DEFAULT_MODEL_PATH
        if os.path.exists(target_path):
            try:
                bundle = joblib.load(target_path)
                self.bundle = bundle
                self.model = bundle["model"]
                self.scaler = bundle["scaler"]
                self.feature_names = bundle["feature_names"]
                self.class_map = bundle["class_map"]
                self.rev_class_map = bundle["rev_class_map"]
                self.metrics = bundle.get("metrics", {})
                self.version = os.path.basename(target_path).replace(".joblib", "")
                self.is_loaded = True
                print(f"[MODEL LOADED] Successfully loaded model from {target_path}")
            except Exception as e:
                print(f"[MODEL LOAD ERROR] Failed to load model from {target_path}: {e}")
                self.is_loaded = False
        else:
            print(f"[MODEL NOT FOUND] Model file not yet found at {target_path}. Retraining required.")
            self.is_loaded = False

model_manager = ModelManager()

# --- Request / Response Models ---

class PredictRequest(BaseModel):
    stationId: Optional[str] = Field("ST-01", description="Station Identifier")
    soilMoisture: Optional[float] = Field(None, description="Soil Moisture % (0 - 100)")
    temperature: Optional[float] = Field(None, description="Ambient Temperature °C")
    humidity: Optional[float] = Field(None, description="Relative Humidity %")
    acousticScore: Optional[int] = Field(None, description="Piezo acoustic activity score (0 - 100)")
    mfccFeatures: Optional[List[float]] = Field(None, description="13 Mel-Frequency Cepstral Coefficients from ESP32")

class PredictResponse(BaseModel):
    stationId: str
    predictLevel: str
    aiRiskScore: int
    confidence: float
    probabilities: Dict[str, float]
    modelVersion: str
    timestamp: str

# --- Endpoints ---

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "termaguard-ml-microservice",
        "modelLoaded": model_manager.is_loaded,
        "modelVersion": model_manager.version,
        "accuracy": model_manager.metrics.get("accuracy"),
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
    }

@app.get("/model/metrics")
def get_metrics():
    if not model_manager.is_loaded:
        raise HTTPException(status_code=503, detail="Model is not yet loaded.")
    return {
        "modelVersion": model_manager.version,
        "metrics": model_manager.metrics,
        "featureNames": model_manager.feature_names,
        "classMap": model_manager.class_map
    }

@app.post("/predict", response_model=PredictResponse)
def predict(payload: PredictRequest):
    station_id = payload.stationId or "UNKNOWN"
    now_str = time.strftime("%Y-%m-%d %H:%M:%S")

    # Mode 1: Null acoustic score (Heartbeat / Keepalive Mode)
    if payload.acousticScore is None:
        return PredictResponse(
            stationId=station_id,
            predictLevel="Safe",
            aiRiskScore=10,
            confidence=0.99,
            probabilities={"negative": 0.98, "suspected": 0.02, "positive": 0.0},
            modelVersion=model_manager.version,
            timestamp=now_str
        )

    # Mode 2: Model loaded and MFCC features provided
    if model_manager.is_loaded and payload.mfccFeatures and len(payload.mfccFeatures) >= 13:
        try:
            # 13 MFCCs
            mfcc_13 = np.array(payload.mfccFeatures[:13], dtype=np.float32)
            
            # acoustic_score, spectral_centroid, zero_crossing_rate
            ac_score = float(payload.acousticScore)
            # Default / estimated baseline for spectral centroid (typical 2200Hz) & ZCR (0.08)
            sc = 2200.0 + (ac_score * 10.0)
            zcr = 0.08 + (ac_score * 0.001)
            
            feat_vec = np.hstack([mfcc_13, [ac_score, sc, zcr]]).reshape(1, -1)
            feat_scaled = model_manager.scaler.transform(feat_vec)
            
            probs = model_manager.model.predict_proba(feat_scaled)[0]
            # Class indices: 0: negative, 1: suspected, 2: positive
            prob_dict = {
                "negative": round(float(probs[0]), 4),
                "suspected": round(float(probs[1]), 4),
                "positive": round(float(probs[2]), 4)
            }
            
            # Calculate continuous AI Risk Score (0 - 100)
            # Weighted formula: Positive * 100 + Suspected * 40
            raw_risk = (prob_dict["positive"] * 100.0) + (prob_dict["suspected"] * 40.0)
            
            # Environmental amplification: High moisture (>65%) in presence of suspected/positive activity
            if payload.soilMoisture and payload.soilMoisture >= 65.0:
                if prob_dict["positive"] > 0.3 or prob_dict["suspected"] > 0.4:
                    raw_risk = min(100.0, raw_risk * 1.1 + 5.0)
                    
            ai_risk_score = int(round(min(100.0, max(5.0, raw_risk))))
            
            # Categorize into Predict Level
            if ai_risk_score >= 70 or prob_dict["positive"] >= 0.55:
                predict_level = "High"
            elif ai_risk_score >= 45 or prob_dict["suspected"] >= 0.50:
                predict_level = "Medium"
            elif ai_risk_score >= 25:
                predict_level = "Low"
            else:
                predict_level = "Safe"
                
            confidence = round(float(max(probs)), 4)
            
            return PredictResponse(
                stationId=station_id,
                predictLevel=predict_level,
                aiRiskScore=ai_risk_score,
                confidence=confidence,
                probabilities=prob_dict,
                modelVersion=model_manager.version,
                timestamp=now_str
            )
        except Exception as e:
            print(f"[PREDICTION ERROR] {e}. Falling back to rule-based evaluation.")

    # Mode 3: Heuristic fallback (if model not yet ready or incomplete MFCCs)
    ac = payload.acousticScore or 10
    if ac >= 75:
        lvl = "High"
        risk = min(98, 75 + int((ac - 75) * 0.9))
        p_pos, p_susp, p_neg = 0.85, 0.12, 0.03
    elif ac >= 50:
        lvl = "Medium"
        risk = 50 + int((ac - 50) * 0.9)
        p_pos, p_susp, p_neg = 0.20, 0.65, 0.15
    elif ac >= 25:
        lvl = "Low"
        risk = 25 + int((ac - 25) * 0.9)
        p_pos, p_susp, p_neg = 0.05, 0.30, 0.65
    else:
        lvl = "Safe"
        risk = max(5, int(ac * 0.5))
        p_pos, p_susp, p_neg = 0.01, 0.09, 0.90

    return PredictResponse(
        stationId=station_id,
        predictLevel=lvl,
        aiRiskScore=risk,
        confidence=0.88,
        probabilities={"negative": p_neg, "suspected": p_susp, "positive": p_pos},
        modelVersion=f"{model_manager.version}-fallback",
        timestamp=now_str
    )

def run_retrain_job():
    print("[BACKGROUND TASK] Starting model retraining...")
    from train_model import train_and_evaluate
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "training_material"))
    model_path = os.path.join(MODEL_DIR, "termite_classifier_v1.joblib")
    meta_path = os.path.join(MODEL_DIR, "metadata.json")
    cache_file = os.path.abspath(os.path.join(os.path.dirname(__file__), "data", "extracted_features.npz"))
    
    train_and_evaluate(base_dir, model_path, meta_path, cache_file)
    model_manager.load_model(model_path)
    print("[BACKGROUND TASK] Model retraining complete and hot-reloaded into memory!")

@app.post("/retrain")
def retrain_model(background_tasks: BackgroundTasks):
    background_tasks.add_task(run_retrain_job)
    return {
        "status": "QUEUED",
        "message": "Model retraining job initiated asynchronously in background. Active model remains online.",
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
