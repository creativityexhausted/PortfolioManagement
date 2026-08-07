package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.AuthResponse;
import com.example.portfoliomanager.dto.ApiDtos.LoginRequest;
import com.example.portfoliomanager.dto.ApiDtos.RegisterRequest;
import com.example.portfoliomanager.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    void registerDelegatesToServiceAndReturnsResponse() {
        RegisterRequest request = new RegisterRequest("demo-user", "StrongPass123!");
        AuthResponse expected = new AuthResponse("jwt-token", "Bearer", 86_400_000L);
        when(authService.register(request)).thenReturn(expected);

        AuthResponse actual = controller.register(request);

        assertThat(actual).isEqualTo(expected);
        verify(authService).register(request);
    }

    @Test
    void loginDelegatesToServiceAndReturnsResponse() {
        LoginRequest request = new LoginRequest("demo-user", "StrongPass123!");
        AuthResponse expected = new AuthResponse("jwt-token", "Bearer", 86_400_000L);
        when(authService.login(request)).thenReturn(expected);

        AuthResponse actual = controller.login(request);

        assertThat(actual).isEqualTo(expected);
        verify(authService).login(request);
    }
}
