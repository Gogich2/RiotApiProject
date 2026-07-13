package org.main.refresh.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.main.refresh.entity.PlayerRefreshJobEntity;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRefreshJobRepository extends JpaRepository<PlayerRefreshJobEntity, UUID> {

    Optional<PlayerRefreshJobEntity> findFirstByPuuidAndStateInOrderByRequestedAtDesc(
            String puuid,
            Collection<RefreshState> states
    );

    Optional<PlayerRefreshJobEntity> findFirstByPuuidAndSourceOrderByRequestedAtDesc(
            String puuid,
            RefreshSource source
    );

    Optional<PlayerRefreshJobEntity> findFirstByPuuidOrderByRequestedAtDesc(String puuid);
}
