package com.rseelabs.subsync.modules.auth;

import com.rseelabs.subsync.modules.auth.dto.AuthResponse;
import com.rseelabs.subsync.modules.auth.dto.LoginRequest;
import com.rseelabs.subsync.modules.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for User Authentication")
@SecurityRequirements()
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Login user with email and password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Verify OTP for email validation")
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody com.rseelabs.subsync.modules.auth.dto.VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @Operation(summary = "Resend OTP to email")
    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@Valid @RequestBody com.rseelabs.subsync.modules.auth.dto.ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @Operation(summary = "Send OTP for forgot password")
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgetPassword(@Valid @RequestBody com.rseelabs.subsync.modules.auth.dto.ForgetPasswordRequest request) {
        return ResponseEntity.ok(authService.forgetPassword(request));
    }

    @Operation(summary = "Reset password using OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody com.rseelabs.subsync.modules.auth.dto.ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @Operation(summary = "Get a new JWT using refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<com.rseelabs.subsync.modules.auth.dto.TokenRefreshResponse> refreshToken(@Valid @RequestBody com.rseelabs.subsync.modules.auth.dto.TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
