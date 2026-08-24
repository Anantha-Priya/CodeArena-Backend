package com.codearena.security;

import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof of the guide's Phase 5 verify checklist against a path that requires
 * authentication but has no controller yet (every real controller is still to come in
 * later phases) - a 401 with no token and a 404 (not 401/403) with a valid one is exactly
 * "rejected, then unlocked by a real token" without needing the Phase 6 login endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtAuthenticationIntegrationTest {

    private static final String PROTECTED_PATH = "/api/does-not-exist-yet";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void noTokenIsRejectedWithUnauthorized() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenFromARealUserPassesAuthentication() throws Exception {
        User user = userRepository.save(User.builder()
            .username("jwt-it-user")
            .email("jwt-it-user@codearena.com")
            .password(passwordEncoder.encode("Passw0rd!"))
            .role(Role.USER)
            .build());

        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .authorities("ROLE_USER")
            .build();
        String token = jwtService.generateToken(userDetails);

        // No handler is mapped to this path, so a valid, authenticated request reaches
        // Spring MVC's dispatcher and gets a 404 - proving it got past the security filter
        // chain rather than being rejected by it.
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

}
