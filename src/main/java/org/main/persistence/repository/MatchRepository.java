package org.main.persistence.repository;

import java.util.List;
import org.main.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, String> {

    List<MatchEntity> findTop20ByOrderByFetchedAtDesc();

    @Query(value = """
            SELECT m.match_id
            FROM raw.matches m
            LEFT JOIN raw.match_timeline_raw tr ON tr.match_id = m.match_id
            WHERE tr.match_id IS NULL
            ORDER BY m.fetched_at ASC
            """, nativeQuery = true)
    List<String> findMatchIdsWithoutTimelineRaw();

    @Query(value = """
            SELECT tr.match_id
            FROM raw.match_timeline_raw tr
            LEFT JOIN raw.match_timeline_frames f ON f.match_id = tr.match_id
            WHERE f.match_id IS NULL
            ORDER BY tr.fetched_at ASC
            """, nativeQuery = true)
    List<String> findTimelineRawIdsWithoutFrames();

    @Query(value = """
            SELECT tr.match_id
            FROM raw.match_timeline_raw tr
            LEFT JOIN raw.match_timeline_events e ON e.match_id = tr.match_id
            WHERE e.match_id IS NULL
            ORDER BY tr.fetched_at ASC
            """, nativeQuery = true)
    List<String> findTimelineRawIdsWithoutEvents();

    @Query(value = """
            SELECT COUNT(*)
            FROM raw.matches m
            LEFT JOIN raw.match_timeline_raw tr ON tr.match_id = m.match_id
            WHERE tr.match_id IS NULL
            """, nativeQuery = true)
    long countMatchesWithoutTimelineRaw();

    @Query(value = """
            SELECT COUNT(*)
            FROM raw.match_timeline_raw tr
            LEFT JOIN raw.match_timeline_frames f ON f.match_id = tr.match_id
            WHERE f.match_id IS NULL
            """, nativeQuery = true)
    long countTimelinesWithoutFrames();

    @Query(value = """
            SELECT COUNT(*)
            FROM raw.match_timeline_raw tr
            LEFT JOIN raw.match_timeline_events e ON e.match_id = tr.match_id
            WHERE e.match_id IS NULL
            """, nativeQuery = true)
    long countTimelinesWithoutEvents();
}