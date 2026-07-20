package com.rseelabs.subsync.modules.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rseelabs.subsync.core.config.JwtAuthenticationFilter;
import com.rseelabs.subsync.core.config.RateLimitFilter;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false) // Disabling security filters for controller logic testing
public class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionRepository subscriptionRepository;

    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockBean
    private RateLimitFilter rateLimitFilter;

    private User mockUser;
    private Subscription mockSubscription;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");

        mockSubscription = new Subscription();
        mockSubscription.setId(UUID.randomUUID());
        mockSubscription.setMerchantName("Netflix");
        mockSubscription.setUser(mockUser);
        mockSubscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetAllSubscriptions() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findAllByUser(mockUser)).thenReturn(List.of(mockSubscription));

        mockMvc.perform(get("/api/v1/subscriptions")
                
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantName").value("Netflix"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetSubscriptionDetails() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findById(mockSubscription.getId())).thenReturn(Optional.of(mockSubscription));

        mockMvc.perform(get("/api/v1/subscriptions/" + mockSubscription.getId())
                
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantName").value("Netflix"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldFailToGetSubscriptionOfOtherUser() throws Exception {
        User otherUser = new User();
        otherUser.setId(2L);
        
        Subscription otherSubscription = new Subscription();
        otherSubscription.setId(UUID.randomUUID());
        otherSubscription.setUser(otherUser);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findById(otherSubscription.getId())).thenReturn(Optional.of(otherSubscription));

        mockMvc.perform(get("/api/v1/subscriptions/" + otherSubscription.getId())
                
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldCancelSubscription() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findById(mockSubscription.getId())).thenReturn(Optional.of(mockSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(mockSubscription);

        mockMvc.perform(put("/api/v1/subscriptions/" + mockSubscription.getId() + "/cancel")
                
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
