package org.main.persistence.repository;

import org.main.persistence.entity.MatchTimelineFrameEntity;
import org.main.persistence.entity.MatchTimelineFrameId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchTimelineFrameRepository
        extends JpaRepository<MatchTimelineFrameEntity, MatchTimelineFrameId> {

    long countByMatchId(String matchId);

    void deleteByMatchId(String matchId);
}