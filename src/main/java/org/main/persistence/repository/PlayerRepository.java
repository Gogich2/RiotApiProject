package org.main.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.main.persistence.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, String> {

    List<PlayerEntity> findByGameNameContainingIgnoreCase(String gameName);

    Optional<PlayerEntity> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);
}