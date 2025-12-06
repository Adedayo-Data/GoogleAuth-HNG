package com.hng.googleAuth.base;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Home {

    @GetMapping("/")
    public ResponseEntity<String> home(){
        return ResponseEntity.status(HttpStatus.OK).body("Welcome to the base App");
    }
}
