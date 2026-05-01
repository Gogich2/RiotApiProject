package org.main.persistence.repository;

import org.main.persistence.entity.MatchTimelineEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchTimelineEventRepository extends JpaRepository<MatchTimelineEventEntity, Long> {

    long countByMatchId(String matchId);

    void deleteByMatchId(String matchId);
}