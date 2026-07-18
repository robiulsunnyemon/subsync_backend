package com.rseelabs.subsync.modules.auth;

import com.rseelabs.subsync.core.service.EmailService;
import com.rseelabs.subsync.core.util.JwtUtils;
import com.rseelabs.subsync.modules.auth.dto.AuthResponse;
import com.rseelabs.subsync.modules.auth.dto.LoginRequest;
import com.rseelabs.subsync.modules.auth.dto.RegisterRequest;
import com.rseelabs.subsync.modules.auth.dto.VerifyOtpRequest;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final org.springframework.core.env.Environment env;

    private String generate4DigitOtp() {
        Random random = new Random();
        int otp = 1000 + random.nextInt(9000);
        return String.valueOf(otp);
    }

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new com.rseelabs.subsync.core.exception.EmailAlreadyExistsException("Email already exists");
        }

        String otp = generate4DigitOtp();

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isEmailVerified(false)
                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .role(com.rseelabs.subsync.modules.user.Role.USER)
                .build();
                
        userRepository.save(user);
        
        emailService.sendOtpEmail(user.getEmail(), otp);
        
        return AuthResponse.builder()
                .token(null)
                .message("OTP sent to your email. Please verify.")
                .build();
    }

    public AuthResponse resendOtp(com.rseelabs.subsync.modules.auth.dto.ResendOtpRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.rseelabs.subsync.core.exception.UserNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        String otp = generate4DigitOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);

        return AuthResponse.builder()
                .token(null)
                .message("A new OTP has been sent to your email.")
                .build();
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.rseelabs.subsync.core.exception.UserNotFoundException("User not found"));
                
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }
        
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new com.rseelabs.subsync.core.exception.InvalidOtpException("OTP has expired");
        }
        
        if (!user.getOtp().equals(request.getOtp())) {
            throw new com.rseelabs.subsync.core.exception.InvalidOtpException("Invalid OTP");
        }
        
        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        
        var jwtToken = jwtUtils.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken.getToken())
                .message("Email verified successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.rseelabs.subsync.core.exception.UserNotFoundException("User not found"));
                
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Please verify your email before logging in.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var jwtToken = jwtUtils.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken.getToken())
                .message("Login successful")
                .build();
    }

    public AuthResponse logout() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            refreshTokenService.deleteByUserId(user.getId());
        }
        return AuthResponse.builder()
                .token(null)
                .message("Logout successful")
                .build();
    }

    public AuthResponse forgetPassword(com.rseelabs.subsync.modules.auth.dto.ForgetPasswordRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.rseelabs.subsync.core.exception.UserNotFoundException("User not found"));

        String otp = generate4DigitOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);

        return AuthResponse.builder()
                .token(null)
                .message("Password reset OTP has been sent to your email.")
                .build();
    }

    public AuthResponse verifyForgetPasswordOtp(com.rseelabs.subsync.modules.auth.dto.VerifyResetOtpRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.rseelabs.subsync.core.exception.UserNotFoundException("User not found"));
        
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new com.rseelabs.subsync.core.exception.InvalidOtpException("OTP has expired");
        }
        
        if (!user.getOtp().equals(request.getOtp())) {
            throw new com.rseelabs.subsync.core.exception.InvalidOtpException("Invalid OTP");
        }
        
        String token = java.util.UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        
        return AuthResponse.builder()
                .token(token)
                .message("OTP verified. Use this token to reset password.")
                .build();
    }

    public AuthResponse resetPassword(com.rseelabs.subsync.modules.auth.dto.ResetPasswordRequest request) {
        var user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
        
        return AuthResponse.builder()
                .token(null)
                .message("Password has been reset successfully.")
                .build();
    }

    public com.rseelabs.subsync.modules.auth.dto.TokenRefreshResponse refreshToken(com.rseelabs.subsync.modules.auth.dto.TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateToken(user);
                    return com.rseelabs.subsync.modules.auth.dto.TokenRefreshResponse.builder()
                            .token(token)
                            .refreshToken(requestRefreshToken)
                            .build();
                })
                .orElseThrow(() -> new com.rseelabs.subsync.core.exception.TokenRefreshException("Refresh token is not in database!"));
    }

    public AuthResponse socialLogin(com.rseelabs.subsync.modules.auth.dto.SocialLoginRequest request) {
        if (request.getProvider() == com.rseelabs.subsync.modules.user.AuthProvider.GOOGLE) {
            return handleGoogleLogin(request.getIdToken());
        }
        throw new RuntimeException("Unsupported social provider: " + request.getProvider());
    }

    private AuthResponse handleGoogleLogin(String idTokenString) {
        try {
            String clientId = this.env.getProperty("google.client.id");

            com.google.api.client.http.HttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport();
            com.google.api.client.json.JsonFactory jsonFactory = new com.google.api.client.json.gson.GsonFactory();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier = new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(java.util.Collections.singletonList(clientId))
                    .build();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String googleId = payload.getSubject();

                User user = userRepository.findByEmail(email).orElse(null);
                if (user == null) {
                    user = User.builder()
                            .fullName(name)
                            .email(email)
                            .password(null) // Password is not required for social login
                            .isEmailVerified(true)
                            .role(com.rseelabs.subsync.modules.user.Role.USER)
                            .provider(com.rseelabs.subsync.modules.user.AuthProvider.GOOGLE)
                            .providerId(googleId)
                            .profileImage(pictureUrl)
                            .build();
                    userRepository.save(user);
                } else {
                    if (user.getProvider() == null || user.getProvider() == com.rseelabs.subsync.modules.user.AuthProvider.LOCAL) {
                        user.setProvider(com.rseelabs.subsync.modules.user.AuthProvider.GOOGLE);
                        user.setProviderId(googleId);
                        user.setEmailVerified(true);
                        userRepository.save(user);
                    }
                }

                var jwtToken = jwtUtils.generateToken(user);
                var refreshToken = refreshTokenService.createRefreshToken(user.getId());

                return AuthResponse.builder()
                        .token(jwtToken)
                        .refreshToken(refreshToken.getToken())
                        .message("Social login successful")
                        .build();

            } else {
                throw new RuntimeException("Invalid ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify social login token: " + e.getMessage());
        }
    }
}
