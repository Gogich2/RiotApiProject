package org.main.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.main.persistence.entity.PlayerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, String> {

    List<PlayerEntity> findByGameNameContainingIgnoreCase(String gameName);

    Optional<PlayerEntity> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);

    Optional<PlayerEntity> findTopByOrderByUpdatedAtDesc();

    @Query("""
            select player
            from PlayerEntity player
            where player.puuid is not null
              and player.puuid <> ''
              and player.profileIconId is null
            order by player.updatedAt asc, player.puuid asc
            """)
    List<PlayerEntity> findPlayersMissingProfiles(Pageable pageable);

    @Query(value = """
            select player.*
            from raw.players player
            where player.puuid is not null
              and player.puuid <> ''
              and not exists (
                  select 1
                  from raw.league_entries entry
                  where entry.puuid = player.puuid
              )
            order by player.updated_at asc, player.puuid asc
            """, nativeQuery = true)
    List<PlayerEntity> findPlayersMissingRanks(Pageable pageable);

    @Query(value = """
            select *
            from raw.players
            order by last_crawl_attempt_at asc nulls first,
                     created_at asc,
                     puuid asc
            limit 1
            """, nativeQuery = true)
    Optional<PlayerEntity> findNextCrawlCandidate();

    @Modifying
    @Transactional
    @Query("""
            update PlayerEntity player
            set player.lastCrawlAttemptAt = :attemptedAt
            where player.puuid = :puuid
            """)
    int updateLastCrawlAttemptAt(
            @Param("puuid") String puuid,
            @Param("attemptedAt") OffsetDateTime attemptedAt
    );

}
