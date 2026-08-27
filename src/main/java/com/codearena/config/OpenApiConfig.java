package com.codearena.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Paste the token returned by POST /api/auth/login (no \"Bearer \" prefix needed here - Swagger adds it)."
)
public class OpenApiConfig {

    /**
     * Declared here (not via class-level @Tag on each controller) so every operation carries
     * exactly one tag - a class-level @Tag on ContestController would merge onto the
     * leaderboard endpoint's explicit @Operation(tags = "Leaderboard") instead of being
     * overridden by it, putting that one operation under two tags.
     */
    @Bean
    public OpenAPI codeArenaOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CodeArena API")
                .description("REST API for a competitive coding-contest platform: register, "
                    + "browse problems, join contests, submit solutions, and see a live leaderboard. "
                    + "v1 does not execute submitted code - submissions are persisted as data with a status/score.")
                .version("v1"))
            .tags(List.of(
                new Tag().name("Authentication").description("Registration and login. No auth token required."),
                new Tag().name("Problems").description("Problem catalog. Writes are admin-only; reads need any valid token."),
                new Tag().name("Contests").description("Contest CRUD, problem association, timing, and joining. Writes are admin-only except join."),
                new Tag().name("Submissions").description("v1 does not execute code - submissions are persisted as data with a caller-supplied status/score."),
                new Tag().name("Leaderboard").description("Contest rankings, computed from accepted submissions."),
                new Tag().name("Users").description("The caller's own profile.")
            ));
    }

}
