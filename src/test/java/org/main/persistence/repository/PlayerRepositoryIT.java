package org.main.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlayerRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").
            withInitScript("player-repository-it.sql");

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void limitsMissingProfileCandidatesInDatabaseOrder() {
        assertThat(playerRepository.findPlayersMissingProfiles(PageRequest.of(0, 1))).
                extracting(player -> player.getPuuid()).
                containsExactly("profile-missing-old");
    }

    @Test
    void excludesPlayersThatAlreadyHaveRankRows() {
        assertThat(playerRepository.findPlayersMissingRanks(PageRequest.of(0, 10))).
                extracting(player -> player.getPuuid()).
                doesNotContain("rank-present").
                contains("profile-missing-old", "profile-missing-new", "profile-present");
    }
}
