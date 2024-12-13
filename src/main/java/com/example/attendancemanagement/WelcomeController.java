package com.example.attendancemanagement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {
    @GetMapping("/welcome")
    public String welcome()
    {
        return "welcome rohan ahmad spring";
    }

}
