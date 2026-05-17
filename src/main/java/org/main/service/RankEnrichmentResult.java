package org.main.service;

import java.util.List;
import org.main.persistence.entity.LeagueEntryEntity;

public record RankEnrichmentResult(
        List<LeagueEntryEntity> entries,
        boolean changed
) {
    public boolean hasEntries() {
        return entries != null && !entries.isEmpty();
    }
}