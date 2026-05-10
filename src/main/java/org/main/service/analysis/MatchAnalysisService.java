package org.main.service.analysis;

public interface MatchAnalysisService {

    int processNewMatches(int limit);

    void processMatch(String matchId);
}