package com.codearena.controller;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.Role;
import com.codearena.entity.Submission;
import com.codearena.entity.SubmissionStatus;
import com.codearena.entity.User;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the problem-delete FK-cascade fix at the HTTP layer (real security filter chain and
 * a real transaction) rather than as a mocked unit test - same approach as
 * ContestControllerDeleteIntegrationTest for the analogous contest-delete fix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProblemControllerDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private ContestProblemRepository contestProblemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

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

    private User saveUser(String slug, Role role) {
        return userRepository.save(User.builder()
            .username(slug)
            .email(slug + "@codearena.com")
            .password(passwordEncoder.encode("Passw0rd!"))
            .role(role)
            .build());
    }

    private Contest saveContest(String slug) {
        return contestRepository.save(Contest.builder()
            .title(slug)
            .description("desc")
            .startTime(Instant.now().minusSeconds(60))
            .endTime(Instant.now().plusSeconds(3600))
            .build());
    }

    private Problem saveProblem(String slug) {
        return problemRepository.save(Problem.builder()
            .title(slug)
            .description("desc")
            .difficulty(Difficulty.EASY)
            .topic("Arrays")
            .constraints("x")
            .inputFormat("x")
            .outputFormat("x")
            .sampleInput("x")
            .sampleOutput("x")
            .build());
    }

    @Test
    void problemWithNoSubmissionsOrAttachmentsDeletesCleanly() throws Exception {
        User admin = saveUser("pd-it-admin-clean", Role.ADMIN);
        Problem problem = saveProblem("pd-it-clean-problem");

        mockMvc.perform(delete("/api/problems/" + problem.getId())
                .header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isNoContent());

        assertThat(problemRepository.findById(problem.getId())).isEmpty();
    }

    @Test
    void problemAttachedToAContestWithNoSubmissionsIsAutoDetachedThenDeleted() throws Exception {
        User admin = saveUser("pd-it-admin-detach", Role.ADMIN);
        Contest contest = saveContest("pd-it-detach-contest");
        Problem problem = saveProblem("pd-it-detach-problem");
        contestProblemRepository.save(ContestProblem.builder().contest(contest).problem(problem).build());

        mockMvc.perform(delete("/api/problems/" + problem.getId())
                .header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isNoContent());

        assertThat(problemRepository.findById(problem.getId())).isEmpty();
        assertThat(contestProblemRepository.existsByContestIdAndProblemId(contest.getId(), problem.getId())).isFalse();
        // The contest itself is untouched - only the association is gone.
        assertThat(contestRepository.findById(contest.getId())).isPresent();
    }

    @Test
    void problemWithAPracticeSubmissionCannotBeDeleted() throws Exception {
        User admin = saveUser("pd-it-admin-practice", Role.ADMIN);
        User submitter = saveUser("pd-it-practice-submitter", Role.USER);
        Problem problem = saveProblem("pd-it-practice-problem");
        Submission submission = submissionRepository.save(Submission.builder()
            .user(submitter)
            .problem(problem)
            .contest(null)
            .language("java")
            .sourceCode("code")
            .status(SubmissionStatus.ACCEPTED)
            .score(100)
            .build());

        // Asserting the actual response body here, not just the status: this is exactly the
        // {status, message} shape ApiError (frontend) is constructed from, and getErrorMessage()
        // reads err.message straight out of it - so this pins down the wire contract the
        // frontend's existing generic-fallback error handling relies on with no code change.
        mockMvc.perform(delete("/api/problems/" + problem.getId())
                .header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(
                "Cannot delete problem " + problem.getId() + ": submissions exist against it"));

        assertThat(problemRepository.findById(problem.getId())).isPresent();
        assertThat(submissionRepository.findById(submission.getId())).isPresent();
    }

    @Test
    void problemWithAContestSubmissionCannotBeDeleted() throws Exception {
        User admin = saveUser("pd-it-admin-contest-sub", Role.ADMIN);
        User submitter = saveUser("pd-it-contest-submitter", Role.USER);
        Contest contest = saveContest("pd-it-contest-sub-contest");
        Problem problem = saveProblem("pd-it-contest-sub-problem");
        contestProblemRepository.save(ContestProblem.builder().contest(contest).problem(problem).build());
        Submission submission = submissionRepository.save(Submission.builder()
            .user(submitter)
            .problem(problem)
            .contest(contest)
            .language("java")
            .sourceCode("code")
            .status(SubmissionStatus.WRONG_ANSWER)
            .score(0)
            .build());

        mockMvc.perform(delete("/api/problems/" + problem.getId())
                .header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(
                "Cannot delete problem " + problem.getId() + ": submissions exist against it"));

        assertThat(problemRepository.findById(problem.getId())).isPresent();
        assertThat(submissionRepository.findById(submission.getId())).isPresent();
    }

    @Test
    void nonAdminDeletingAProblemIsRejectedWithForbidden() throws Exception {
        User regularUser = saveUser("pd-it-user", Role.USER);
        Problem problem = saveProblem("pd-it-403-problem");

        mockMvc.perform(delete("/api/problems/" + problem.getId())
                .header("Authorization", "Bearer " + tokenFor(regularUser, Role.USER)))
            .andExpect(status().isForbidden());

        assertThat(problemRepository.findById(problem.getId())).isPresent();
    }

    @Test
    void deletingANonexistentProblemReturnsNotFound() throws Exception {
        User admin = saveUser("pd-it-admin-404", Role.ADMIN);

        mockMvc.perform(delete("/api/problems/999999999")
                .header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isNotFound());
    }

}
