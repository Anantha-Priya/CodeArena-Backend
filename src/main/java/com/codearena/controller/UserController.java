package com.codearena.controller;

import com.codearena.dto.UserProfileResponse;
import com.codearena.exception.ErrorResponse;
import com.codearena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "Get the caller's own profile",
        description = "username/role/rating/problemsSolved/contestsJoined - role and rating are "
            + "stored, problemsSolved/contestsJoined are computed live from submission/participant data.",
        tags = {"Users"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile data",
                content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getMyProfile(userDetails.getUsername());
    }

    @Operation(
        summary = "List every user with their stats (admin only)",
        description = "Same fields and computation as GET /api/users/me, one entry per user. "
            + "No email/password data. Not paginated - fine at this project's scale.",
        tags = {"Users"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Every user's profile data",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserProfileResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping
    public List<UserProfileResponse> getAllUsers() {
        return userService.getAllUsers();
    }

}
