package org.main.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.PlatformShard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueEntryRepository extends JpaRepository<LeagueEntryEntity, Long> {

    Optional<LeagueEntryEntity> findByPlatformAndPuuidAndQueueType(
            PlatformShard platform,
            String puuid,
            String queueType
    );

    boolean existsByPuuid(String puuid);

    List<LeagueEntryEntity> findByPuuidOrderByQueueTypeAsc(String puuid);
}