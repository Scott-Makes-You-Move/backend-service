package nl.optifit.backendservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class BackendServiceApplication implements CommandLineRunner {

    public static final String TEST_USER_SUB = "3dac8d2f-a6d1-4e8e-8920-eeb8e77963a5";

    public static void main(String[] args) {
        SpringApplication.run(BackendServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

    }
}