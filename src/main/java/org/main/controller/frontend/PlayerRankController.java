package org.main.controller.frontend;

import java.util.List;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.LeagueEntrySnapshotEntity;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.LeagueEntrySnapshotRepository;
import org.main.service.RankEnrichmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class PlayerRankController {

    private final LeagueEntryRepository leagueEntryRepository;

    private final LeagueEntrySnapshotRepository leagueEntrySnapshotRepository;

    private final RankEnrichmentService rankEnrichmentService;

    public PlayerRankController(LeagueEntryRepository leagueEntryRepository,
                                LeagueEntrySnapshotRepository leagueEntrySnapshotRepository,
                                RankEnrichmentService rankEnrichmentService) {
        this.leagueEntryRepository = leagueEntryRepository;
        this.leagueEntrySnapshotRepository = leagueEntrySnapshotRepository;
        this.rankEnrichmentService = rankEnrichmentService;
    }

    @GetMapping("/api/players/{puuid}/ranks")
    public List<LeagueEntryEntity> getRanks(@PathVariable String puuid) {
        return leagueEntryRepository.findByPuuidOrderByQueueTypeAsc(puuid);
    }

    @PostMapping("/api/players/{puuid}/refresh-ranks")
    public List<LeagueEntryEntity> refreshRanks(@PathVariable String puuid) {
        rankEnrichmentService.enrichRanksForPuuidEuw(puuid);
        return leagueEntryRepository.findByPuuidOrderByQueueTypeAsc(puuid);
    }

    @GetMapping("/api/players/{puuid}/rank-history")
    public List<LeagueEntrySnapshotEntity> getRankHistory(@PathVariable String puuid) {
        return leagueEntrySnapshotRepository.findByPuuidOrderBySyncedAtDesc(puuid);
    }
}