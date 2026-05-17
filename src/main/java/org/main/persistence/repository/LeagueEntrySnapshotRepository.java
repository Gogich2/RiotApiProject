package org.main.persistence.repository;

import java.util.List;
import org.main.persistence.entity.LeagueEntrySnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueEntrySnapshotRepository extends JpaRepository<LeagueEntrySnapshotEntity, Long> {

    List<LeagueEntrySnapshotEntity> findByPuuidOrderBySyncedAtDesc(String puuid);

    List<LeagueEntrySnapshotEntity> findByPuuidAndQueueTypeOrderBySyncedAtDesc(String puuid, String queueType);
}