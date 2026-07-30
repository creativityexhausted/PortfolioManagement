package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.AppUser;
import com.example.portfoliomanager.dto.ApiDtos.AuthResponse;
import com.example.portfoliomanager.dto.ApiDtos.LoginRequest;
import com.example.portfoliomanager.dto.ApiDtos.RegisterRequest;
import com.example.portfoliomanager.repository.AppUserRepository;
import com.example.portfoliomanager.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository repository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already registered");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        repository.save(user);
        return response(toUserDetails(user));
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password()));
        AppUser user = repository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        return response(toUserDetails(user));
    }

    private AuthResponse response(UserDetails userDetails) {
        return new AuthResponse(jwtService.generateToken(userDetails), "Bearer", jwtService.getExpirationMs());
    }

    private UserDetails toUserDetails(AppUser user) {
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }
}
