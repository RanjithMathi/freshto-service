package com.ra.freshChickenAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FreshChickenApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreshChickenApiApplication.class, args);
	}

}
