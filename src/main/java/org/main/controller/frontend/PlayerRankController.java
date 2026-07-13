package org.main.controller.frontend;

import java.util.List;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.LeagueEntrySnapshotEntity;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.LeagueEntrySnapshotRepository;
import org.main.refresh.dto.PlayerRefreshStatusDto;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.service.PlayerRefreshCoordinator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class PlayerRankController {

    private final LeagueEntryRepository leagueEntryRepository;

    private final LeagueEntrySnapshotRepository leagueEntrySnapshotRepository;

    private final PlayerRefreshCoordinator refreshCoordinator;

    public PlayerRankController(LeagueEntryRepository leagueEntryRepository,
                                LeagueEntrySnapshotRepository leagueEntrySnapshotRepository,
                                PlayerRefreshCoordinator refreshCoordinator) {
        this.leagueEntryRepository = leagueEntryRepository;
        this.leagueEntrySnapshotRepository = leagueEntrySnapshotRepository;
        this.refreshCoordinator = refreshCoordinator;
    }

    @GetMapping("/api/players/{puuid}/ranks")
    public List<LeagueEntryEntity> getRanks(@PathVariable String puuid) {
        return leagueEntryRepository.findByPuuidOrderByQueueTypeAsc(puuid);
    }

    @PostMapping("/api/players/{puuid}/refresh-ranks")
    public ResponseEntity<PlayerRefreshStatusDto> refreshRanks(@PathVariable String puuid) {
        return ResponseEntity.accepted().body(refreshCoordinator.enqueue(puuid, RefreshSource.MANUAL));
    }

    @GetMapping("/api/players/{puuid}/rank-history")
    public List<LeagueEntrySnapshotEntity> getRankHistory(@PathVariable String puuid) {
        return leagueEntrySnapshotRepository.findByPuuidOrderBySyncedAtDesc(puuid);
    }
}
