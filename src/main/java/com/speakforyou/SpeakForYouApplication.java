package com.speakforyou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.speakforyou.mapper")
public class SpeakForYouApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeakForYouApplication.class, args);
    }
}
