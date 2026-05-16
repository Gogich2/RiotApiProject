package org.main.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.PlatformShard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueEntryRepository extends JpaRepository<LeagueEntryEntity, Long> {

    List<LeagueEntryEntity> findByPuuidOrderByQueueTypeAsc(String puuid);

    Optional<LeagueEntryEntity> findByPlatformAndSummonerIdAndQueueType(
            PlatformShard platform,
            String summonerId,
            String queueType
    );

    boolean existsByPuuid(String puuid);
}