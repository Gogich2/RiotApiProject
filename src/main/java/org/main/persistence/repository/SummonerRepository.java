package org.main.persistence.repository;

import java.util.Optional;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.entity.SummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummonerRepository extends JpaRepository<SummonerEntity, String> {

    Optional<SummonerEntity> findByPlatformAndPuuid(PlatformShard platform, String puuid);
}