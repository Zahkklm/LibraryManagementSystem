package com.librarysystem.authservice.controller;
import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.security.JwtUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserServiceClient userServiceClient;
  private final JwtUtils jwtUtils;

  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
    boolean isValid = userServiceClient.validateCredentials(request);

    if (isValid) {
      String role = userServiceClient.getUserRole(request.getEmail());
      String token = jwtUtils.generateToken(request.getEmail(), role);
      return ResponseEntity.ok(new JwtResponse(token));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
