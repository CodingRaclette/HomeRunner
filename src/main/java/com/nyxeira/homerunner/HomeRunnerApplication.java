package com.nyxeira.homerunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // nécessaire à ReminderScheduler (cf. conception 4.8/5.5)
public class HomeRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeRunnerApplication.class, args);
    }

}
