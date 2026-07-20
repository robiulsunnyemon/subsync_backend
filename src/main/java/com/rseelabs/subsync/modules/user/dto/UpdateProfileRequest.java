package com.rseelabs.subsync.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Size(max = 100, message = "Business name cannot exceed 100 characters")
    private String businessName;

    @Size(max = 50, message = "VAT number cannot exceed 50 characters")
    private String vatNumber;
    
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    private String bio;
}
