package com.termiguard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.termiguard.mapper")
public class TermiguardApplication {

    public static void main(String[] args) {
        SpringApplication.run(TermiguardApplication.class, args);
    }
}
