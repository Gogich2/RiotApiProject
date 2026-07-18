package org.main.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StaticPlayerDashboardTest {

    private static final Path STATIC_ROOT = Path.of("src", "main", "resources", "static");

    @Test
    void playerPageProvidesDashboardFirstContainersAndActions() throws IOException {
        String html = read("player.html");

        assertThat(html).contains("id=\"playerDashboardRanks\"");
        assertThat(html).contains("id=\"playerFreshness\"");
        assertThat(html).contains("id=\"playerAnalysisQueue\"");
        assertThat(html).contains("data-player-queue=\"420\"");
        assertThat(html).contains("data-player-queue=\"440\"");
        assertThat(html).contains("id=\"recentForm5\"");
        assertThat(html).contains("id=\"recentForm10\"");
        assertThat(html).contains("id=\"recentForm20\"");
        assertThat(html).contains("id=\"championPoolHealth\"");
        assertThat(html).contains("id=\"dashboardChampionPool\"");
        assertThat(html).contains("id=\"playerPriorities\"");
        assertThat(html).contains("id=\"saveProfileButton\"");
        assertThat(html).contains("id=\"refreshPlayerButton\"");
        assertThat(html).contains("id=\"playerRefreshStatus\" aria-live=\"polite\"");
    }

    @Test
    void dashboardScriptUsesSingleDashboardFetchAndExplicitRefreshStates() throws IOException {
        String js = read("js/player.js");

        assertThat(js).contains("api.getPlayerDashboard(puuid)");
        assertThat(js).contains("api.getPlayerDashboard(puuid, queueId)");
        assertThat(js).contains("renderPlayerMatches(dashboard.recentMatches || [])");
        assertThat(js).doesNotContain("api.getPlayerMatches(");
        assertThat(js).contains("api.getPlayerRefreshStatus(puuid)");
        assertThat(js).contains("QUEUED");
        assertThat(js).contains("RUNNING");
        assertThat(js).contains("RATE_LIMITED");
        assertThat(js).contains("Cached data");
        assertThat(js).contains("account.html?returnTo=");
        assertThat(js).contains("api.markSavedProfileViewed(saved.id)");
    }

    @Test
    void dashboardStylesAreResponsiveTouchFriendlyAndReducedMotionAware() throws IOException {
        String css = read("css/player.css");

        assertThat(css).contains(".player-dashboard");
        assertThat(css).contains("min-height: 44px;");
        assertThat(css).contains("@media (max-width: 768px)");
        assertThat(css).contains("@media (prefers-reduced-motion: reduce)");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(STATIC_ROOT.resolve(relativePath));
    }
}
