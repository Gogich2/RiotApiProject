package org.main.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.account.dto.SavedProfileDto;
import org.main.account.entity.SavedProfileEntity;
import org.main.account.repository.SavedProfileRepository;
import org.main.exception.NotFoundException;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.PlayerRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavedProfileServiceTest {

    private static final UUID USER_ID = UUID.fromString("23da8d39-fc2e-460c-b6dc-28ab0821cc82");

    private static final UUID SAVED_ID = UUID.fromString("ab438de5-4983-4182-9e67-ae52187cd38e");

    private static final Instant INSTANT = Instant.parse("2026-07-13T05:00:00Z");

    @Mock
    private SavedProfileRepository savedProfileRepository;

    @Mock
    private PlayerRepository playerRepository;

    private SavedProfileService savedProfileService;

    @BeforeEach
    void setUp() {
        savedProfileService = new SavedProfileService(
                savedProfileRepository,
                playerRepository,
                Clock.fixed(INSTANT, ZoneOffset.UTC)
        );
    }

    @Test
    void savesAnyExistingPublicProfileWithPrivateLabel() {
        when(playerRepository.findById("known-puuid")).thenReturn(Optional.of(player("known-puuid")));
        when(savedProfileRepository.findByUserIdAndPuuid(USER_ID, "known-puuid")).thenReturn(Optional.empty());
        when(savedProfileRepository.save(any(SavedProfileEntity.class))).
                thenAnswer(invocation -> invocation.getArgument(0));

        SavedProfileDto saved = savedProfileService.save(USER_ID, "known-puuid", "  Main account  ");

        assertThat(saved.puuid()).isEqualTo("known-puuid");
        assertThat(saved.personalLabel()).isEqualTo("Main account");
        assertThat(saved.gameName()).isEqualTo("Player");
        assertThat(saved.savedAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
    }

    @Test
    void rejectsUnknownPuuidAndDuplicateBookmark() {
        when(playerRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> savedProfileService.save(USER_ID, "unknown", null)).
                isInstanceOf(NotFoundException.class);

        when(playerRepository.findById("known-puuid")).thenReturn(Optional.of(player("known-puuid")));
        when(savedProfileRepository.findByUserIdAndPuuid(USER_ID, "known-puuid")).
                thenReturn(Optional.of(savedProfile(USER_ID, SAVED_ID, "known-puuid", false)));
        assertThatThrownBy(() -> savedProfileService.save(USER_ID, "known-puuid", null)).
                isInstanceOf(SavedProfileService.DuplicateSavedProfileException.class);
    }

    @Test
    void listsProfilesInRepositoryLastViewedOrder() {
        SavedProfileEntity recent = savedProfile(USER_ID, SAVED_ID, "recent", false);
        SavedProfileEntity older = savedProfile(
                USER_ID,
                UUID.fromString("6c261d38-372c-4783-a0c1-c10c8078c262"),
                "older",
                false
        );
        when(savedProfileRepository.findByUserIdOrderByLastViewedAtDesc(USER_ID)).
                thenReturn(List.of(recent, older));
        when(playerRepository.findById("recent")).thenReturn(Optional.of(player("recent")));
        when(playerRepository.findById("older")).thenReturn(Optional.of(player("older")));

        assertThat(savedProfileService.list(USER_ID)).extracting(SavedProfileDto::puuid).
                containsExactly("recent", "older");
    }

    @Test
    void anotherUsersSavedIdIsNotVisibleForUpdateOrDelete() {
        when(savedProfileRepository.findByIdAndUserId(SAVED_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedProfileService.update(USER_ID, SAVED_ID, "Label", false)).
                isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> savedProfileService.delete(USER_ID, SAVED_ID)).
                isInstanceOf(NotFoundException.class);
    }

    @Test
    void choosingDefaultClearsPreviousDefaultForSameUser() {
        SavedProfileEntity target = savedProfile(USER_ID, SAVED_ID, "target", false);
        SavedProfileEntity previous = savedProfile(
                USER_ID,
                UUID.fromString("6c261d38-372c-4783-a0c1-c10c8078c262"),
                "previous",
                true
        );
        when(savedProfileRepository.findByIdAndUserId(SAVED_ID, USER_ID)).thenReturn(Optional.of(target));
        when(savedProfileRepository.findByUserIdOrderByLastViewedAtDesc(USER_ID)).
                thenReturn(List.of(previous, target));
        when(playerRepository.findById("target")).thenReturn(Optional.of(player("target")));
        when(savedProfileRepository.save(target)).thenReturn(target);

        SavedProfileDto updated = savedProfileService.update(USER_ID, SAVED_ID, " Focus ", true);

        assertThat(previous.isDefault()).isFalse();
        assertThat(updated.isDefault()).isTrue();
        assertThat(updated.personalLabel()).isEqualTo("Focus");
        verify(savedProfileRepository).saveAll(List.of(previous));
    }

    @Test
    void deletesOnlyUserScopedBookmark() {
        SavedProfileEntity saved = savedProfile(USER_ID, SAVED_ID, "known-puuid", false);
        when(savedProfileRepository.findByIdAndUserId(SAVED_ID, USER_ID)).thenReturn(Optional.of(saved));

        savedProfileService.delete(USER_ID, SAVED_ID);

        verify(savedProfileRepository).delete(saved);
    }

    @Test
    void marksSavedProfileAsRecentlyViewed() {
        SavedProfileEntity saved = savedProfile(USER_ID, SAVED_ID, "known-puuid", false);
        saved.setLastViewedAt(OffsetDateTime.ofInstant(INSTANT.minusSeconds(86_400), ZoneOffset.UTC));
        when(savedProfileRepository.findByIdAndUserId(SAVED_ID, USER_ID)).thenReturn(Optional.of(saved));

        savedProfileService.markViewed(USER_ID, SAVED_ID);

        assertThat(saved.getLastViewedAt()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        verify(savedProfileRepository).save(saved);
    }

    private SavedProfileEntity savedProfile(UUID userId, UUID id, String puuid, boolean isDefault) {
        SavedProfileEntity saved = new SavedProfileEntity();
        saved.setId(id);
        saved.setUserId(userId);
        saved.setPuuid(puuid);
        saved.setDefault(isDefault);
        saved.setSavedAt(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        saved.setLastViewedAt(OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC));
        return saved;
    }

    private PlayerEntity player(String puuid) {
        PlayerEntity player = new PlayerEntity();
        player.setPuuid(puuid);
        player.setGameName("Player");
        player.setTagLine("EUW");
        player.setProfileIconId(123);
        return player;
    }
}
