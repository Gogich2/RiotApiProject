package org.main.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.main.RiotPractice;
import org.main.persistence.repository.MatchRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.main.client.RiotApiClient;
import org.springframework.boot.test.mock.mockito.MockBean;


@CucumberContextConfiguration
@SpringBootTest(
        classes = RiotPractice.class,
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

    @MockBean
    private RiotApiClient riotApiClient;

    @MockBean
    private MatchRepository matchRepository;
}
