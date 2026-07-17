package com.rseelabs.subsync.modules.auth;

import com.rseelabs.subsync.core.exception.EmailAlreadyExistsException;
import com.rseelabs.subsync.core.exception.UserNotFoundException;
import com.rseelabs.subsync.core.service.EmailService;
import com.rseelabs.subsync.modules.auth.dto.AuthResponse;
import com.rseelabs.subsync.modules.auth.dto.LoginRequest;
import com.rseelabs.subsync.modules.auth.dto.RegisterRequest;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import com.rseelabs.subsync.core.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Test User");
        registerRequest.setEmail("test@test.com");
        registerRequest.setPassword("123456");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("123456");

        testUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encoded_password")
                .isEmailVerified(true)
                .build();
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("OTP sent to your email. Please verify.", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendOtpEmail(eq("test@test.com"), anyString());
    }

    @Test
    void testRegisterFailsWhenEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken(any(User.class))).thenReturn("jwt_token");
        
        RefreshToken mockToken = new RefreshToken();
        mockToken.setToken("refresh_token");
        when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(mockToken);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("Login successful", response.getMessage());
        
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLoginFailsWhenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(loginRequest));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void testLoginFailsWhenEmailNotVerified() {
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Please verify your email before logging in.", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }
}
