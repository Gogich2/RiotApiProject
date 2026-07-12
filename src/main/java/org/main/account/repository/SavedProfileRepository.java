package org.main.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.main.account.entity.SavedProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedProfileRepository extends JpaRepository<SavedProfileEntity, UUID> {

    List<SavedProfileEntity> findByUserIdOrderByLastViewedAtDesc(UUID userId);

    Optional<SavedProfileEntity> findByUserIdAndPuuid(UUID userId, String puuid);

    Optional<SavedProfileEntity> findByIdAndUserId(UUID id, UUID userId);
}
