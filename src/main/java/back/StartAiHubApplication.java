package back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class StartAiHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartAiHubApplication.class, args);
    }
}