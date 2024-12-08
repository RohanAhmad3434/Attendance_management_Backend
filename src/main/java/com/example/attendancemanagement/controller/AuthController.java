package com.example.attendancemanagement.controller;

import com.example.attendancemanagement.entity.User;
import com.example.attendancemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // Fetch the user by username
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);

        // If user not found or passwords don't match
        if (user == null || !user.getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        // Return the user role and id in the response
        return ResponseEntity.ok(new LoginResponse(user.getId(), user.getRole().toString()));
    }

    // Inner static class to handle login request data
    public static class LoginRequest {
        private String username;
        private String password;

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // Inner static class to handle login response data (role and id)
    public static class LoginResponse {
        private Long id;
        private String role;

        // Constructor
        public LoginResponse(Long id, String role) {
            this.id = id;
            this.role = role;
        }

        // Getters
        public Long getId() {
            return id;
        }

        public String getRole() {
            return role;
        }

        // Setters
        public void setId(Long id) {
            this.id = id;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
