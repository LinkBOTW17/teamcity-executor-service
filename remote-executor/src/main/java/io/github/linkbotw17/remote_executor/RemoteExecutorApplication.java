package io.github.linkbotw17.remote_executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RemoteExecutorApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemoteExecutorApplication.class, args);
	}

}
