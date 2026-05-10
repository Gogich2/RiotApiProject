package org.main.persistence.repository;

import org.main.persistence.entity.MatchTimelineRawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchTimelineRawRepository extends JpaRepository<MatchTimelineRawEntity, String> {
}