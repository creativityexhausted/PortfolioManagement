package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.AppUser;
import com.example.portfoliomanager.dto.ApiDtos.AuthResponse;
import com.example.portfoliomanager.dto.ApiDtos.LoginRequest;
import com.example.portfoliomanager.dto.ApiDtos.RegisterRequest;
import com.example.portfoliomanager.repository.AppUserRepository;
import com.example.portfoliomanager.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesUsernamePersistsUserAndReturnsJwt() {
        RegisterRequest request = new RegisterRequest("  DemoUser  ", "StrongPass123!");
        when(repository.existsByUsername("demouser")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("encoded-password");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("demouser");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(86_400_000L);
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("demo", "StrongPass123!");
        when(repository.existsByUsername("demo")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already registered");

        verify(repository, never()).save(any());
    }

    @Test
    void loginAuthenticatesNormalizedUsernameAndReturnsJwt() {
        LoginRequest request = new LoginRequest("  DemoUser  ", "StrongPass123!");
        AppUser user = new AppUser();
        user.setUsername("demouser");
        user.setPassword("encoded-password");

        when(repository.findByUsername("demouser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("demouser", "StrongPass123!"));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresInMs()).isEqualTo(3_600_000L);
    }

    @Test
    void loginPropagatesAuthenticationFailure() {
        LoginRequest request = new LoginRequest("demo", "bad-pass");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
    }
}