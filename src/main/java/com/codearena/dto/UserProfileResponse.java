package com.codearena.dto;

import com.codearena.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String username;
    private Role role;
    private int rating;
    private long problemsSolved;
    private long contestsJoined;

}
