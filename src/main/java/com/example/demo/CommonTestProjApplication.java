package com.example.demo;

import com.example.demo.service.ChatGPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
public class CommonTestProjApplication {

	/*@Autowired
	private static ChatGPTService chatGPTService;
	*//*CommonTestProjApplication(ChatGPTService chatGPTService){
		this.chatGPTService=new ChatGPTService();
	}
*/
	public static void main(String[] args) throws IOException {


		SpringApplication.run(CommonTestProjApplication.class, args);

	}


	/*@Bean
	CommandLineRunner run(ChatGPTService chatGPTService) {
		return args -> {
			String response = chatGPTService.askChatGPT("Write a Java Hello World program");
			System.out.println("ChatGPT Response:\n" + response);
		};
	}
*/

}
