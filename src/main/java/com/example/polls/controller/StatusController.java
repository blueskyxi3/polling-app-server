package com.example.polls.controller;


import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/_status/healthz")
public class StatusController {

    @GetMapping
    public String healthCheck() {
        
        String java_home = System.getenv("JAVA_HOME");
        String version = System.getenv("APP_VERSION");
        System.out.println("version is ---------> "+version);
        System.out.println("java_home is ---------> "+java_home);
        
        return "UP";
    }

}
