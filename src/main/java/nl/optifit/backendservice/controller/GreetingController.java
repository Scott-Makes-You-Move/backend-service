package nl.optifit.backendservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RequestMapping("/api/v1/greeting")
@RestController
public class GreetingController {

    @GetMapping
    public String greeting(@RequestParam Optional<String> name) {
        log.debug("Greeting requested: '{}'", name);
        return "Hello " + name.orElse("World");
    }
}