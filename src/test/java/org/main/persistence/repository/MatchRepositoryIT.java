package org.main.persistence.repository;

import org.junit.jupiter.api.Test;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.entity.RegionRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MatchRepositoryIT {

    @Autowired
    MatchRepository matchRepository;

    @Test
    void save_then_existsById_then_findById_work_with_real_postgres() {
        String matchId = "IT_" + UUID.randomUUID();

        MatchEntity e = new MatchEntity();
        e.setMatchId(matchId);
        e.setRegion(RegionRoute.europe);
        e.setPlatform(PlatformShard.EUW1);
        e.setRawMatchJson("{\"it\":true}");
        e.setFetchedAt(OffsetDateTime.now());

        matchRepository.saveAndFlush(e);

        assertTrue(matchRepository.existsById(matchId));

        MatchEntity loaded = matchRepository.findById(matchId).orElseThrow();
        assertEquals(matchId, loaded.getMatchId());
        assertEquals(PlatformShard.EUW1, loaded.getPlatform());
        assertEquals(RegionRoute.europe, loaded.getRegion());
        assertEquals("{\"it\":true}", loaded.getRawMatchJson());
        assertNotNull(loaded.getFetchedAt());
    }
}
