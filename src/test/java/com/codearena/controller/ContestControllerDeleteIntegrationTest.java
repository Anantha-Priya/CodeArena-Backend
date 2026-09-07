package com.codearena.controller;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestParticipant;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.Role;
import com.codearena.entity.Submission;
import com.codearena.entity.SubmissionStatus;
import com.codearena.entity.User;
import com.codearena.repository.ContestParticipantRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the SecurityConfig rule for HttpMethod.DELETE on /api/contests/** actually runs -
 * same HTTP-layer approach as UserControllerIntegrationTest, since the 403 is enforced
 * declaratively, before either controller method is ever invoked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContestControllerDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ContestProblemRepository contestProblemRepository;

    @Autowired
    private ContestParticipantRepository contestParticipantRepository;

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
    void nonAdminDeletingAContestIsRejectedWithForbidden() throws Exception {
        User regularUser = saveUser("cd-it-user", Role.USER);
        Contest contest = saveContest("cd-it-delete-target");

        mockMvc.perform(delete("/api/contests/" + contest.getId())
                .header("Authorization", "Bearer " + tokenFor(regularUser, Role.USER)))
            .andExpect(status().isForbidden());

        // Rejected at the security filter - the contest must still be there.
        assertThat(contestRepository.findById(contest.getId())).isPresent();
    }

    @Test
    void nonAdminDetachingAProblemIsRejectedWithForbidden() throws Exception {
        User regularUser = saveUser("cd-it-user-detach", Role.USER);
        Contest contest = saveContest("cd-it-detach-target");
        Problem problem = saveProblem("cd-it-detach-problem");
        contestProblemRepository.save(ContestProblem.builder().contest(contest).problem(problem).build());

        mockMvc.perform(delete("/api/contests/" + contest.getId() + "/problems/" + problem.getId())
                .header("Authorization", "Bearer " + tokenFor(regularUser, Role.USER)))
            .andExpect(status().isForbidden());

        assertThat(contestProblemRepository.existsByContestIdAndProblemId(contest.getId(), problem.getId())).isTrue();
    }

    @Test
    void adminDeletingANonexistentContestGetsNotFoundNotServerError() throws Exception {
        User admin = saveUser("cd-it-admin-404", Role.ADMIN);

        mockMvc.perform(delete("/api/contests/999999999")
                .header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isNotFound());
    }

    @Test
    void deletingAContestWithParticipantsProblemsAndSubmissionsCascadesWithoutViolatingAnyForeignKey() throws Exception {
        User admin = saveUser("cd-it-admin-cascade", Role.ADMIN);
        User participant = saveUser("cd-it-participant", Role.USER);
        Contest contest = saveContest("cd-it-cascade-contest");
        Problem problem = saveProblem("cd-it-cascade-problem");

        contestProblemRepository.save(ContestProblem.builder().contest(contest).problem(problem).build());
        contestParticipantRepository.save(ContestParticipant.builder().user(participant).contest(contest).build());
        Submission submission = submissionRepository.save(Submission.builder()
            .user(participant)
            .problem(problem)
            .contest(contest)
            .language("java")
            .sourceCode("code")
            .status(SubmissionStatus.ACCEPTED)
            .score(100)
            .build());

        mockMvc.perform(delete("/api/contests/" + contest.getId())
                .header("Authorization", "Bearer " + tokenFor(admin, Role.ADMIN)))
            .andExpect(status().isNoContent());

        assertThat(contestRepository.findById(contest.getId())).isEmpty();
        assertThat(contestProblemRepository.findByContestId(contest.getId())).isEmpty();
        assertThat(contestParticipantRepository.findByContestId(contest.getId())).isEmpty();

        // The submission itself survives and reverts to a practice submission - not deleted.
        Submission reloaded = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(reloaded.getContest()).isNull();
    }

    @Test
    void detachingThenReattachingTheSameProblemSucceeds() throws Exception {
        User admin = saveUser("cd-it-admin-reattach", Role.ADMIN);
        Contest contest = saveContest("cd-it-reattach-contest");
        Problem problem = saveProblem("cd-it-reattach-problem");
        contestProblemRepository.save(ContestProblem.builder().contest(contest).problem(problem).build());

        String adminAuth = "Bearer " + tokenFor(admin, Role.ADMIN);

        mockMvc.perform(delete("/api/contests/" + contest.getId() + "/problems/" + problem.getId())
                .header("Authorization", adminAuth))
            .andExpect(status().isNoContent());

        // The unique (contest_id, problem_id) constraint must not block re-attaching after a
        // real row delete.
        mockMvc.perform(post("/api/contests/" + contest.getId() + "/problems/" + problem.getId())
                .header("Authorization", adminAuth))
            .andExpect(status().isCreated());

        assertThat(contestProblemRepository.existsByContestIdAndProblemId(contest.getId(), problem.getId())).isTrue();
    }

}
