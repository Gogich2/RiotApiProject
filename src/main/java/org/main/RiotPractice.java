package org.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RiotPractice {

	private static final Logger log = LoggerFactory.getLogger(RiotPractice.class);

	public static void main(String[] args) {
		log.info("Starting RiotApiPractice application");
		SpringApplication.run(RiotPractice.class, args);
		log.info("RiotApiPractice application started successfully");
	}
}