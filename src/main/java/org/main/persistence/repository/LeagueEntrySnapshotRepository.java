package org.main.persistence.repository;

import java.util.List;
import org.main.persistence.entity.LeagueEntrySnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueEntrySnapshotRepository extends JpaRepository<LeagueEntrySnapshotEntity, Long> {

    List<LeagueEntrySnapshotEntity> findByPuuidOrderBySyncedAtDesc(String puuid);
}