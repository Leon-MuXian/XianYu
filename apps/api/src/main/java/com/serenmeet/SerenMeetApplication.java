package com.serenmeet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 闲遇 MVP 后端应用入口。
 */
@SpringBootApplication
@MapperScan("com.serenmeet.**.mapper")
@EnableScheduling
public class SerenMeetApplication {

  public static void main(String[] args) {
    SpringApplication.run(SerenMeetApplication.class, args);
  }
}
