package com.rseelabs.subsync.modules.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rseelabs.subsync.core.config.JwtAuthenticationFilter;
import com.rseelabs.subsync.core.config.RateLimitFilter;
import com.rseelabs.subsync.modules.user.UserService;
import com.rseelabs.subsync.modules.user.dto.UpdateProfileRequest;
import com.rseelabs.subsync.modules.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockBean
    private RateLimitFilter rateLimitFilter;

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetProfile() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .fullName("Test User")
                .email("test@example.com")
                .build();

        when(userService.getProfile(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/v1/settings/profile")
                
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldUpdateProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "My Biz", "VAT123", "Bio");
        UserProfileResponse response = UserProfileResponse.builder()
                .fullName("Updated Name")
                .email("test@example.com")
                .build();

        when(userService.updateProfile(eq("test@example.com"), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/settings/profile")
                
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }
}
