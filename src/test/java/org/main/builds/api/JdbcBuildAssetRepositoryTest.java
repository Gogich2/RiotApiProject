package org.main.builds.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@SuppressWarnings({"unchecked", "rawtypes"})
class JdbcBuildAssetRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private JdbcBuildAssetRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcBuildAssetRepository(jdbc);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).
                thenReturn(List.of());
    }

    @Test
    void usesOneQueryPerNonEmptyAssetIdBatch() {
        repository.findChampions(List.of(22, 55));
        verify(jdbc, times(1)).query(
                anyString(), any(RowMapper.class), any(Object[].class));
        clearInvocations(jdbc);

        repository.findItems(List.of(1055, 3006));
        verify(jdbc, times(1)).query(
                anyString(), any(RowMapper.class), any(Object[].class));
        clearInvocations(jdbc);

        repository.findRunes(List.of(8000, 8005));
        verify(jdbc, times(1)).query(
                anyString(), any(RowMapper.class), any(Object[].class));
        clearInvocations(jdbc);

        repository.findSpells(List.of(4, 14));
        verify(jdbc, times(1)).query(
                anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void skipsQueriesForEmptyBatches() {
        repository.findChampions(List.of());
        repository.findItems(List.of());
        repository.findRunes(List.of());
        repository.findSpells(List.of());

        verifyNoInteractions(jdbc);
    }
}
