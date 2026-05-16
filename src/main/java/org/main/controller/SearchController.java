package org.main.controller;

import java.util.List;
import org.main.persistence.entity.LeagueEntryEntity;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.PlayerRepository;
import org.main.service.RankEnrichmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контролер для пошуку вже збережених у базі даних гравців і матчів.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PlayerRepository playerRepository;

    private final MatchRepository matchRepository;

    private final LeagueEntryRepository leagueEntryRepository;

    private final RankEnrichmentService rankEnrichmentService;

    public SearchController(PlayerRepository playerRepository,
                            MatchRepository matchRepository,
                            LeagueEntryRepository leagueEntryRepository,
                            RankEnrichmentService rankEnrichmentService) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.leagueEntryRepository = leagueEntryRepository;
        this.rankEnrichmentService = rankEnrichmentService;
    }

    @GetMapping("/players")
    public List<PlayerEntity> searchPlayers(@RequestParam String query) {
        return playerRepository.findByGameNameContainingIgnoreCase(query);
    }

    @GetMapping("/players/riot-id")
    public PlayerEntity findPlayerByRiotId(@RequestParam String gameName,
                                           @RequestParam String tagLine) {
        PlayerEntity player = playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine).
                orElseThrow(() -> new IllegalArgumentException(
                        "Player not found: " + gameName + "#" + tagLine
                ));

        rankEnrichmentService.enrichRanksForPuuidEuw(player.getPuuid());

        return player;
    }

    @GetMapping("/players/{puuid}")
    public PlayerEntity findPlayerByPuuid(@PathVariable String puuid) {
        PlayerEntity player = playerRepository.findById(puuid).
                orElseThrow(() -> new IllegalArgumentException(
                        "Player not found by puuid: " + puuid
                ));

        rankEnrichmentService.enrichRanksForPuuidEuw(player.getPuuid());

        return player;
    }

    @GetMapping("/players/{puuid}/ranks")
    public List<LeagueEntryEntity> findPlayerRanks(@PathVariable String puuid) {
        rankEnrichmentService.enrichRanksForPuuidEuw(puuid);
        return leagueEntryRepository.findByPuuidOrderByQueueTypeAsc(puuid);
    }

    @GetMapping("/matches/{matchId}")
    public MatchEntity findMatchById(@PathVariable String matchId) {
        return matchRepository.findById(matchId).
                orElseThrow(() -> new IllegalArgumentException(
                        "Match not found: " + matchId
                ));
    }

    @GetMapping("/matches/latest")
    public List<MatchEntity> findLatestMatches() {
        return matchRepository.findTop20ByOrderByFetchedAtDesc();
    }
}