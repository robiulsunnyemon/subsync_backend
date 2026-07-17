package com.rseelabs.subsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubsyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubsyncApplication.class, args);
	}

}
