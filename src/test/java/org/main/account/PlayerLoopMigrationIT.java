package org.main.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "app.scheduler.data-integrity.enabled=false",
        "app.scheduler.match-analysis.enabled=false"
})
class PlayerLoopMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAllPlayerLoopTables() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'app'
                  and table_name in (
                      'app_user',
                      'oauth_identity',
                      'user_session',
                      'account_action_token',
                      'saved_profile',
                      'player_refresh_job'
                  )
                """, Integer.class);

        assertThat(count).isEqualTo(6);
    }
}
