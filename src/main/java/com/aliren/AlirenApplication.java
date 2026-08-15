package com.aliren;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aliren")
public class AlirenApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlirenApplication.class, args);
    }
}
