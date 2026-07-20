package com.rseelabs.subsync.modules.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rseelabs.subsync.core.config.JwtAuthenticationFilter;
import com.rseelabs.subsync.core.config.RateLimitFilter;
import com.rseelabs.subsync.modules.bank.provider.BankProviderFactory;
import com.rseelabs.subsync.modules.bank.provider.TinkProviderImpl;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BankController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankProviderFactory providerFactory;

    @MockBean
    private TinkProviderImpl tinkProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockBean
    private RateLimitFilter rateLimitFilter;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGenerateAuthLink() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(providerFactory.getProvider(BankConnection.BankProvider.TINK)).thenReturn(tinkProvider);
        when(tinkProvider.generateAuthLink(anyString(), eq(mockUser.getId().toString()), eq("GB"))).thenReturn("https://auth.tink.com/link");

        mockMvc.perform(get("/api/v1/banks/auth-link")
                .param("provider", "TINK")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUrl").value("https://auth.tink.com/link"));
    }
}
