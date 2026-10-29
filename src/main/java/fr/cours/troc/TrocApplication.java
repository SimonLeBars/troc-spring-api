package fr.cours.troc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TrocApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrocApplication.class, args);
	}

}
