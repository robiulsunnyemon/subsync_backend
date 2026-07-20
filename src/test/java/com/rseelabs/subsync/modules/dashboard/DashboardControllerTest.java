package com.rseelabs.subsync.modules.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rseelabs.subsync.core.config.JwtAuthenticationFilter;
import com.rseelabs.subsync.core.config.RateLimitFilter;
import com.rseelabs.subsync.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false) // Disabling security filters for controller logic testing
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockBean
    private RateLimitFilter rateLimitFilter;

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetDashboardSummary() throws Exception {
        DashboardResponse mockResponse = DashboardResponse.builder()
                .totalMonthlyExpense(java.math.BigDecimal.valueOf(150.0))
                .activeSubscriptionsCount(3)
                .upcomingPayments(List.of())
                .build();

        when(dashboardService.getDashboardSummary("test@example.com")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                 // Mocking the principal directly since filters are disabled
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonthlyExpense").value(150.0))
                .andExpect(jsonPath("$.activeSubscriptionsCount").value(3));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturnNotFoundWhenUserNotFound() throws Exception {
        when(dashboardService.getDashboardSummary("test@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/dashboard/summary")
                
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
