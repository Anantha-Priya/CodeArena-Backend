package com.codearena.service;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestParticipant;
import com.codearena.entity.User;
import com.codearena.exception.BadRequestException;
import com.codearena.exception.DuplicateResourceException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestParticipantServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContestParticipantRepository contestParticipantRepository;

    private ContestParticipantService service;

    private User user;

    @BeforeEach
    void setUp() {
        service = new ContestParticipantService(contestRepository, userRepository, contestParticipantRepository);
        user = User.builder().id(1L).email("user@codearena.com").build();
    }

    @Test
    void joiningANonexistentContestThrowsNotFound() {
        when(contestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(99L, "user@codearena.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void joiningAnEndedContestIsRejected() {
        Contest ended = Contest.builder()
            .id(1L)
            .startTime(Instant.now().minus(2, ChronoUnit.HOURS))
            .endTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();
        when(contestRepository.findById(1L)).thenReturn(Optional.of(ended));

        assertThatThrownBy(() -> service.join(1L, "user@codearena.com"))
            .isInstanceOf(BadRequestException.class);

        verify(contestParticipantRepository, never()).save(any());
    }

    @Test
    void joiningTwiceIsRejectedWithDuplicate() {
        Contest active = Contest.builder()
            .id(1L)
            .startTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();
        when(contestRepository.findById(1L)).thenReturn(Optional.of(active));
        when(userRepository.findByEmail("user@codearena.com")).thenReturn(Optional.of(user));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.join(1L, "user@codearena.com"))
            .isInstanceOf(DuplicateResourceException.class);

        verify(contestParticipantRepository, never()).save(any());
    }

    @Test
    void firstJoinOnAnActiveContestSucceeds() {
        Contest active = Contest.builder()
            .id(1L)
            .startTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();
        when(contestRepository.findById(1L)).thenReturn(Optional.of(active));
        when(userRepository.findByEmail("user@codearena.com")).thenReturn(Optional.of(user));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 1L)).thenReturn(false);

        service.join(1L, "user@codearena.com");

        ArgumentCaptor<ContestParticipant> captor = ArgumentCaptor.forClass(ContestParticipant.class);
        verify(contestParticipantRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getContest()).isEqualTo(active);
    }

    @Test
    void joiningAnUpcomingContestSucceeds() {
        Contest upcoming = Contest.builder()
            .id(1L)
            .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
            .build();
        when(contestRepository.findById(1L)).thenReturn(Optional.of(upcoming));
        when(userRepository.findByEmail("user@codearena.com")).thenReturn(Optional.of(user));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 1L)).thenReturn(false);

        service.join(1L, "user@codearena.com");

        verify(contestParticipantRepository, times(1)).save(any());
    }

    @Test
    void hasJoinedReturnsTrueAfterJoining() {
        when(userRepository.findByEmail("user@codearena.com")).thenReturn(Optional.of(user));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 1L)).thenReturn(true);

        assertThat(service.hasJoined(1L, "user@codearena.com")).isTrue();
    }

    @Test
    void hasJoinedReturnsFalseBeforeJoining() {
        when(userRepository.findByEmail("user@codearena.com")).thenReturn(Optional.of(user));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 1L)).thenReturn(false);

        assertThat(service.hasJoined(1L, "user@codearena.com")).isFalse();
    }

    @Test
    void hasJoinedThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findByEmail("ghost@codearena.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.hasJoined(1L, "ghost@codearena.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

}
