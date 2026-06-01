package org.main.service.frontend;

import org.main.dto.frontend.MatchDetailsDto;

public interface MatchDetailsService {

    MatchDetailsDto getMatchDetails(String matchId, String puuid);

}
