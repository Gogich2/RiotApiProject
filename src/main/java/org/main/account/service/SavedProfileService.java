package org.main.account.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.main.account.dto.SavedProfileDto;
import org.main.account.entity.SavedProfileEntity;
import org.main.account.repository.SavedProfileRepository;
import org.main.exception.NotFoundException;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedProfileService {

    private static final Duration VIEW_WRITE_INTERVAL = Duration.ofMinutes(15);

    private final SavedProfileRepository savedProfileRepository;

    private final PlayerRepository playerRepository;

    private final Clock clock;

    public SavedProfileService(
            SavedProfileRepository savedProfileRepository,
            PlayerRepository playerRepository,
            Clock clock
    ) {
        this.savedProfileRepository = savedProfileRepository;
        this.playerRepository = playerRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SavedProfileDto> list(UUID currentUserId) {
        return savedProfileRepository.findByUserIdOrderByLastViewedAtDesc(currentUserId).
                stream().
                map(this::toDto).
                toList();
    }

    @Transactional
    public SavedProfileDto save(UUID currentUserId, String puuid, String personalLabel) {
        PlayerEntity player = playerRepository.findById(puuid).
                orElseThrow(() -> new NotFoundException("Player profile not found"));
        if (savedProfileRepository.findByUserIdAndPuuid(currentUserId, puuid).isPresent()) {
            throw new DuplicateSavedProfileException();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        SavedProfileEntity saved = new SavedProfileEntity();
        saved.setId(UUID.randomUUID());
        saved.setUserId(currentUserId);
        saved.setPuuid(player.getPuuid());
        saved.setPersonalLabel(normalizeLabel(personalLabel));
        saved.setDefault(false);
        saved.setSavedAt(now);
        saved.setLastViewedAt(now);
        savedProfileRepository.save(saved);
        return toDto(saved, player);
    }

    @Transactional
    public SavedProfileDto update(
            UUID currentUserId,
            UUID savedProfileId,
            String personalLabel,
            boolean isDefault
    ) {
        SavedProfileEntity saved = findForUser(currentUserId, savedProfileId);
        if (isDefault) {
            List<SavedProfileEntity> previousDefaults = savedProfileRepository.
                    findByUserIdOrderByLastViewedAtDesc(currentUserId).
                    stream().
                    filter(candidate -> candidate.isDefault() && !candidate.getId().equals(savedProfileId)).
                    peek(candidate -> candidate.setDefault(false)).
                    toList();
            savedProfileRepository.saveAll(previousDefaults);
        }
        saved.setPersonalLabel(normalizeLabel(personalLabel));
        saved.setDefault(isDefault);
        savedProfileRepository.save(saved);
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID currentUserId, UUID savedProfileId) {
        savedProfileRepository.delete(findForUser(currentUserId, savedProfileId));
    }

    @Transactional
    public void markViewed(UUID currentUserId, UUID savedProfileId) {
        SavedProfileEntity saved = findForUser(currentUserId, savedProfileId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (saved.getLastViewedAt() == null
                || !saved.getLastViewedAt().plus(VIEW_WRITE_INTERVAL).isAfter(now)) {
            saved.setLastViewedAt(now);
            savedProfileRepository.save(saved);
        }
    }

    private SavedProfileEntity findForUser(UUID currentUserId, UUID savedProfileId) {
        return savedProfileRepository.findByIdAndUserId(savedProfileId, currentUserId).
                orElseThrow(() -> new NotFoundException("Saved profile not found"));
    }

    private SavedProfileDto toDto(SavedProfileEntity saved) {
        PlayerEntity player = playerRepository.findById(saved.getPuuid()).
                orElseThrow(() -> new NotFoundException("Player profile not found"));
        return toDto(saved, player);
    }

    private SavedProfileDto toDto(SavedProfileEntity saved, PlayerEntity player) {
        return new SavedProfileDto(
                saved.getId(),
                saved.getPuuid(),
                player.getGameName(),
                player.getTagLine(),
                player.getProfileIconId(),
                saved.getPersonalLabel(),
                saved.isDefault(),
                saved.getSavedAt(),
                saved.getLastViewedAt()
        );
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return label.strip();
    }

    public static class DuplicateSavedProfileException extends RuntimeException {

        public DuplicateSavedProfileException() {
            super("This profile is already saved.");
        }
    }
}
