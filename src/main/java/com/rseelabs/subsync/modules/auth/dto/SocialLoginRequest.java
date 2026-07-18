package com.rseelabs.subsync.modules.auth.dto;

import com.rseelabs.subsync.modules.user.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SocialLoginRequest {
    @NotBlank(message = "idToken is required")
    private String idToken;

    @NotNull(message = "provider is required")
    private AuthProvider provider;
}
