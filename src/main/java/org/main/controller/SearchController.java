package org.main.controller;

import java.util.List;
import org.main.persistence.entity.MatchEntity;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.PlayerRepository;
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

    public SearchController(PlayerRepository playerRepository, MatchRepository matchRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
    }

    @GetMapping("/players")
    public List<PlayerEntity> searchPlayers(@RequestParam String query) {
        return playerRepository.findByGameNameContainingIgnoreCase(query);
    }

    @GetMapping("/players/riot-id")
    public PlayerEntity findPlayerByRiotId(@RequestParam String gameName,
                                           @RequestParam String tagLine) {
        return playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine).
                orElseThrow(() -> new IllegalArgumentException(
                        "Player not found: " + gameName + "#" + tagLine
                ));
    }

    @GetMapping("/players/{puuid}")
    public PlayerEntity findPlayerByPuuid(@PathVariable String puuid) {
        return playerRepository.findById(puuid).
                orElseThrow(() -> new IllegalArgumentException(
                        "Player not found by puuid: " + puuid
                ));
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