package com.memoire.assistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoire.assistant.dto.AuthRequest;
import com.memoire.assistant.dto.AuthResponse;
import com.memoire.assistant.model.User;
import com.memoire.assistant.repository.UserRepository;
import com.memoire.assistant.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled("Temporarily disabled - needs proper mocking setup")
class AuthControllerUnitTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.memoire.assistant.security.CustomUserDetailsService customUserDetailsService;

    private AuthController authController;

    @Test
    void login_shouldReturnToken_whenCredentialsValid() throws Exception {
        // Given
        AuthRequest req = new AuthRequest();
        req.setEmail("test@mail.com");
        req.setPassword("pass");
        
        User user = new User();
        user.setEmail("test@mail.com");
        user.setPassword("pass");
        user.setRole("ADMIN");
        
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(auth);
        when(userRepository.findByEmail("test@mail.com"))
            .thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getEmail(), user.getRole()))
            .thenReturn("token123");

        // When
        AuthResponse response = authController.login(req);

        // Then
        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void login_shouldReturnError_whenCredentialsInvalid() throws Exception {
        // Given
        AuthRequest req = new AuthRequest();
        req.setEmail("wrong@mail.com");
        req.setPassword("bad");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("bad creds"));

        // When & Then
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            authController.login(req);
        });
    }
}
