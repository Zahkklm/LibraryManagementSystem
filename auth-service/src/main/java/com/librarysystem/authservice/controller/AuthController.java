package com.librarysystem.authservice.controller;
import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.security.JwtUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  @Autowired
  private final UserServiceClient userServiceClient;

  private final JwtUtils jwtUtils;

  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
    boolean isValid = userServiceClient.validateCredentials(request);

    if (isValid) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      String token = jwtUtils.generateToken(authentication);
      return ResponseEntity.ok(new JwtResponse(token));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
