package mk.ukim.finki.wp.lab1b2026;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class Lab1B2026Application {

    public static void main(String[] args) {
        SpringApplication.run(Lab1B2026Application.class, args);
    }

}
