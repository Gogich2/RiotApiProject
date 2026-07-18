package org.main.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class MatchAnalysisServiceImplTest {

    @Test
    void batchesPlayerUpsertsFromParticipants() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MatchAnalysisServiceImpl service = new MatchAnalysisServiceImpl(
                jdbcTemplate,
                new ObjectMapper(),
                mock(TransactionTemplate.class)
        );
        JsonNode participants = new ObjectMapper().readTree("""
                [
                  {"puuid":"one","riotIdGameName":"One","riotIdTagline":"EUW"},
                  {"puuid":"two","riotIdGameName":"Two","riotIdTagline":"EUW"},
                  {"puuid":""}
                ]
                """);

        ReflectionTestUtils.invokeMethod(service, "savePlayersFromParticipants", participants);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batches = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), batches.capture());
        assertThat(batches.getValue()).hasSize(2);
        assertThat(batches.getValue().get(0)).containsExactly("one", "One", "EUW");
        assertThat(batches.getValue().get(1)).containsExactly("two", "Two", "EUW");
    }
}
