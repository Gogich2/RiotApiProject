package org.main.persistence.repository;

import org.main.persistence.entity.IngestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestLogRepository extends JpaRepository<IngestLogEntity, Long> {
}