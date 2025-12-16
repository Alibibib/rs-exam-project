package org.example.demoservice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    @GetMapping("/public")
    public String pub(){
        return "public ok";
    }

    @GetMapping("/private")
    public String priv(@AuthenticationPrincipal Jwt jwt){
        String id = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return "private ok | sub= " + id + " | username = " + username + " | email = " + email;
    }
}
