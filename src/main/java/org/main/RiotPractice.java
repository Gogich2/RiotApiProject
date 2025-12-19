package org.example;

import org.example.controller.SummonerController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;

@SpringBootApplication
public class RiotPractice {
	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(RiotPractice.class, args);
		SummonerController controller = context.getBean(SummonerController.class);
		try{
			ResponseEntity<?> response = controller.getSummoner("Acoomer");
			System.out.println(response.getBody());
		} catch (Exception e){
			System.out.println("Error: " + e.getMessage());
		}
	}
}