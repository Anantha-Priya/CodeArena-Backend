package com.codearena.controller;

import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.UserRepository;
import com.codearena.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The 403 for non-admins is enforced declaratively in SecurityConfig, not in application
 * code, so this is tested at the HTTP layer (real security filter chain) rather than as a
 * SecurityConfig-bypassing unit test - same approach as JwtAuthenticationIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    private static final String PATH = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String tokenFor(User user, Role role) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .authorities("ROLE_" + role.name())
            .build();
        return jwtService.generateToken(userDetails);
    }

    @Test
    void nonAdminIsRejectedWithForbidden() throws Exception {
        User regularUser = userRepository.save(User.builder()
            .username("uc-it-user")
            .email("uc-it-user@codearena.com")
            .password(passwordEncoder.encode("Passw0rd!"))
            .role(Role.USER)
            .build());

        mockMvc.perform(get(PATH).header("Authorization", "Bearer " + tokenFor(regularUser, Role.USER)))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminGetsTheFullListWithCorrectFieldsAndNoCredentialData() throws Exception {
        User admin = userRepository.save(User.builder()
            .username("uc-it-admin")
            .email("uc-it-admin@codearena.com")
            .password(passwordEncoder.encode("Passw0rd!"))
            .role(Role.ADMIN)
            .rating(42)
            .build());
        userRepository.save(User.builder()
            .username("uc-it-other-user")
            .email("uc-it-other-user@codearena.com")
            .password(passwordEncoder.encode("Passw0rd!"))
            .role(Role.USER)
            .build());

        mockMvc.perform(get(PATH).header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.username=='uc-it-admin')].role").value("ADMIN"))
            .andExpect(jsonPath("$[?(@.username=='uc-it-admin')].rating").value(42))
            .andExpect(jsonPath("$[?(@.username=='uc-it-admin')].problemsSolved").value(0))
            .andExpect(jsonPath("$[?(@.username=='uc-it-admin')].contestsJoined").value(0))
            .andExpect(jsonPath("$[?(@.username=='uc-it-other-user')].role").value("USER"))
            .andExpect(jsonPath("$[?(@.username=='uc-it-admin')].password").doesNotExist())
            .andExpect(jsonPath("$[?(@.username=='uc-it-admin')].email").doesNotExist());
    }

}
