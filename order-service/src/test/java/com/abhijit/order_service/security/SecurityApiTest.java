package com.abhijit.order_service.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@SpringBootTest
@ActiveProfiles("test")
class SecurityApiTest {

    private static final String ORDERS = "/orders?customerEmail=nobody@example.com";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String login(String user, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String customToken(JwtEncoder encoder, Instant issuedAt, Instant expiresAt, List<String> roles) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("order-service").subject("customer").issuedAt(issuedAt).expiresAt(expiresAt);
        if (roles != null) {
            claims.claim("roles", roles);
        }
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    // --- login ---

    @Test
    void loginReturnsABearerToken() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"customer\",\"password\":\"test-user-pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void wrongPasswordIsRejectedWith401() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"customer\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void unknownUserGetsTheSameAnswerAsAWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void blankLoginFieldsGive400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- authentication: who are you? (401) ---

    @Test
    void noTokenGives401() throws Exception {
        mockMvc.perform(get(ORDERS)).andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenIsAccepted() throws Exception {
        String token = login("customer", "test-user-pw");
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void aTamperedTokenGives401() throws Exception {
        String token = login("customer", "test-user-pw");
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(token + "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenGives401() throws Exception {
        Instant now = Instant.now();
        String expired = customToken(jwtEncoder, now.minus(2, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS), List.of("USER"));
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithAnotherKeyGives401() throws Exception {
        JwtEncoder foreign = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "a-totally-different-secret-of-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant now = Instant.now();
        String forged = customToken(foreign, now, now.plus(30, ChronoUnit.MINUTES), List.of("ADMIN"));
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void basicAuthIsNoLongerAccepted() throws Exception {
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, "Basic Y3VzdG9tZXI6dGVzdC11c2VyLXB3"))
                .andExpect(status().isUnauthorized());
    }

    // --- authorization: what may you do? (403) ---

    @Test
    void aValidTokenWithoutRolesGives403() throws Exception {
        Instant now = Instant.now();
        String noRoles = customToken(jwtEncoder, now, now.plus(30, ChronoUnit.MINUTES), null);
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(noRoles)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aCustomerCannotCancel() throws Exception {
        String token = login("customer", "test-user-pw");
        mockMvc.perform(post("/orders/999999/cancel").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdminPassesAuthorizationOnCancel() throws Exception {
        String token = login("admin", "test-admin-pw");
        // 404 (order does not exist) proves the request got past the security rules
        mockMvc.perform(post("/orders/999999/cancel").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }
}