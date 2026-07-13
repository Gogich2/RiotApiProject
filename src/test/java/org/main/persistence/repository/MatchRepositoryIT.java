package org.main.persistence.repository;

import org.junit.jupiter.api.Test;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.entity.RegionRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DirtiesContext
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MatchRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").
            withInitScript("match-repository-it.sql");

    @Autowired
    MatchRepository matchRepository;

    @Test
    void saveThenExistsByIdThenFindByIdWorkWithRealPostgres() {
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
