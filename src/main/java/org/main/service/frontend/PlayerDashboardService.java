package org.main.service.frontend;

import org.main.dto.frontend.PlayerDashboardDto;

public interface PlayerDashboardService {

    PlayerDashboardDto getDashboard(String puuid);

    PlayerDashboardDto getDashboard(String puuid, Integer queueId);
}
