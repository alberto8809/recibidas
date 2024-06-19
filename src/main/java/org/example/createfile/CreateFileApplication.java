package org.example.createfile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

//@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
@SpringBootApplication
public class CreateFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreateFileApplication.class, args);
    }

}
