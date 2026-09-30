package com.org.tailSafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TailSafeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TailSafeApplication.class, args);
	}

}
