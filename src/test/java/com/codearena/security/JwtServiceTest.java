package com.codearena.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
        "sArkBmL6uyxXwibilY7XGgTRUh0deTIkinhgXI5dKLzdPfQ45ArSXnFf1deV3cofpgAGEnh4+JMKmOv/5nNVNQ==",
        86400000L
    );

    private final UserDetails user = User.withUsername("test@codearena.com")
        .password("irrelevant-for-token-checks")
        .authorities("ROLE_USER")
        .build();

    @Test
    void generatedTokenIsValidForTheUserItWasIssuedTo() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getUsername());
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsNotValidForADifferentUser() {
        String token = jwtService.generateToken(user);
        UserDetails someoneElse = User.withUsername("other@codearena.com")
            .password("irrelevant")
            .authorities("ROLE_USER")
            .build();

        assertThat(jwtService.isTokenValid(token, someoneElse)).isFalse();
    }

}
