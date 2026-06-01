package org.main.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.main.client.RiotApiClient;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.PlayerRepository;
import org.main.service.CrawlerServiceImpl;
import org.main.service.IngestLogService;
import org.main.service.TimelineIngestService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;


@CucumberContextConfiguration
@SpringBootTest(
        classes = CucumberSpringConfig.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)

public class CucumberSpringConfig {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(CrawlerServiceImpl.class)
    static class TestApplication {
    }

    @MockBean
    private RiotApiClient riotApiClient;

    @MockBean
    private MatchRepository matchRepository;

    @MockBean
    private PlayerRepository playerRepository;

    @MockBean
    private TimelineIngestService timelineIngestService;

    @MockBean
    private IngestLogService ingestLogService;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private JdbcTemplate jdbcTemplate;
}
