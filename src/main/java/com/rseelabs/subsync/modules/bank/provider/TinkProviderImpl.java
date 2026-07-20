package com.rseelabs.subsync.modules.bank.provider;

import com.rseelabs.subsync.modules.bank.BankConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class TinkProviderImpl implements OpenBankingProvider {

    @Value("${tink.client.id}")
    private String clientId;

    @Value("${tink.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TINK_API_URL = "https://api.tink.com";

    @Override
    public BankConnection.BankProvider getProviderType() {
        return BankConnection.BankProvider.TINK;
    }

    @Override
    public String generateAuthLink(String redirectUri, String state, String market) {
        if (market == null || market.isEmpty()) market = "GB";
        return String.format(
                "https://link.tink.com/1.0/transactions/connect-accounts?" +
                        "client_id=%s&redirect_uri=%s&market=%s&locale=en_US&state=%s",
                clientId, redirectUri, market, state
        );
    }

    @Override
    public String exchangeAuthorizationCode(String code) {
        String url = TINK_API_URL + "/api/v1/oauth/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new RuntimeException("Failed to exchange Tink authorization code");
    }

    private String getClientAccessToken() {
        String url = TINK_API_URL + "/api/v1/oauth/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");
        body.add("scope", "providers:read"); // Typically required

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new RuntimeException("Failed to get Tink client access token");
    }

    @Override
    public List<com.rseelabs.subsync.modules.bank.dto.BankProviderDto> fetchProviders(String countryCode) {
        String accessToken = getClientAccessToken();
        String url = TINK_API_URL + "/api/v1/providers?market=" + countryCode;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        
        List<com.rseelabs.subsync.modules.bank.dto.BankProviderDto> providers = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<Map<String, Object>> providersList = (List<Map<String, Object>>) response.getBody().get("providers");
            if (providersList != null) {
                for (Map<String, Object> p : providersList) {
                    com.rseelabs.subsync.modules.bank.dto.BankProviderDto dto = com.rseelabs.subsync.modules.bank.dto.BankProviderDto.builder()
                        .name((String) p.get("name"))
                        .displayName((String) p.get("displayName"))
                        .type((String) p.get("type"))
                        .market(countryCode)
                        .icon("B") // Default icon since Tink usually returns image URLs in 'images'
                        .build();
                    
                    // try to extract an icon
                    Map<String, Object> images = (Map<String, Object>) p.get("images");
                    if (images != null && images.get("icon") != null) {
                        dto.setIcon((String) images.get("icon"));
                    }
                    
                    providers.add(dto);
                }
            }
        }
        return providers;
    }

    @Override
    public List<BankTransactionDTO> fetchTransactions(String accessToken, LocalDate from, LocalDate to) {
        String url = TINK_API_URL + "/data/v2/transactions"; // simplified for example
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        
        List<BankTransactionDTO> dtos = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // Note: In reality, you'd parse the specific JSON structure returned by Tink v2 API
            // For now, this is a simplified stub returning empty to avoid complex JSON mapping logic
            // dtos.add(new BankTransactionDTO(...));
        }
        return dtos;
    }
}
