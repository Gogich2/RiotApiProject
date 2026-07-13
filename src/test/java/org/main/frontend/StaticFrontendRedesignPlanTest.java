package org.main.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StaticFrontendRedesignPlanTest {

    private static final Path STATIC_ROOT = Path.of("src", "main", "resources", "static");

    @Test
    void homePageIncludesMetaSnapshotShellAndRendererHooks() throws IOException {
        String html = readStaticFile("index.html");
        String js = readStaticFile("js/home.js");
        String componentsCss = readStaticFile("css/components.css");

        assertThat(html).contains("id=\"metaSnapshot\"");
        assertThat(html).contains("id=\"homeHeroSummary\"");
        assertThat(js).contains("function renderMetaSnapshot");
        assertThat(js).contains("function buildMetaSnapshotCards");
        assertThat(js).contains("renderMetaSnapshot(overview, leaderboards)");
        assertThat(componentsCss).contains(".meta-snapshot");
        assertThat(componentsCss).contains(".meta-snapshot__card");
        assertThat(componentsCss).contains(".hero-summary-card");
    }

    @Test
    void sharedThemeUsesPurpleAndCreamTokenSystem() throws IOException {
        String baseCss = readStaticFile("css/base.css");

        assertThat(baseCss).contains("--color-bg-app-rgb: 10, 7, 20;");
        assertThat(baseCss).contains("--color-accent: #6800ff;");
        assertThat(baseCss).contains("--color-text-primary: #fff9eb;");
        assertThat(baseCss).contains("--ease-out-strong:");
        assertThat(baseCss).contains("--duration-fast:");
        assertThat(baseCss).doesNotContain("--color-accent: #47c6bb;");
    }

    @Test
    void sharedComponentsExposeSystemWideMicroInteractionHooks() throws IOException {
        String layoutCss = readStaticFile("css/layout.css");
        String componentsCss = readStaticFile("css/components.css");
        String playerCss = readStaticFile("css/player.css");
        String championCss = readStaticFile("css/champion.css");
        String matchCss = readStaticFile("css/match-details.css");

        assertThat(layoutCss).contains(".search__results--visible");
        assertThat(layoutCss).contains("transform: translateY(6px) scale(0.985);");
        assertThat(componentsCss).contains(".button:active");
        assertThat(componentsCss).contains(".hero-summary-card:hover");
        assertThat(playerCss).contains(".player-tabs__button:active");
        assertThat(championCss).contains(".role-sort-button:active");
        assertThat(matchCss).contains(".player-match-card:active");
    }

    @Test
    void listingPagesUseUpdatedPlanShells() throws IOException {
        String playersHtml = readStaticFile("players.html");
        String championsHtml = readStaticFile("champions.html");
        String championsJs = readStaticFile("js/champions.js");

        assertThat(playersHtml).contains("page page--listing");
        assertThat(playersHtml).contains("page-hero page-hero--compact");
        assertThat(playersHtml).contains("leaderboard-grid leaderboard-grid--balanced");
        assertThat(championsHtml).contains("class=\"champion-toolbar\"");
        assertThat(championsHtml).contains("id=\"championRoleButtons\"");
        assertThat(championsHtml).contains("id=\"championFilterInput\"");
        assertThat(championsJs).contains("priority");
        assertThat(championsJs).contains("champions visible across");
    }

    @Test
    void detailPagesUseUpdatedDetailShells() throws IOException {
        String playerHtml = readStaticFile("player.html");
        String championHtml = readStaticFile("champion.html");
        String matchHtml = readStaticFile("match.html");

        assertThat(playerHtml).contains("page page--detail");
        assertThat(playerHtml).contains("hero hero--detail");
        assertThat(playerHtml).contains("stats-grid stats-grid--detail");
        assertThat(championHtml).contains("page page--detail");
        assertThat(championHtml).contains("champion-hero champion-hero--detail");
        assertThat(championHtml).contains("stats-grid stats-grid--detail");
        assertThat(matchHtml).contains("page page--detail");
        assertThat(matchHtml).contains("page-hero page-hero--compact");
        assertThat(matchHtml).contains("Fallback view");
    }

    @Test
    void homePageUsesRiotIdFirstOnboardingWithCsrfAwareRequests() throws IOException {
        String html = readStaticFile("index.html");
        String homeJs = readStaticFile("js/home.js");
        String apiJs = readStaticFile("js/api.js");

        assertThat(html).contains("for=\"riotGameName\"");
        assertThat(html).contains("name=\"gameName\"");
        assertThat(html).contains("for=\"riotTagLine\"");
        assertThat(html).contains("name=\"tagLine\"");
        assertThat(html).contains("id=\"riotIdSubmit\"");
        assertThat(homeJs).contains("player.html?puuid=");
        assertThat(apiJs).contains("/auth/csrf");
        assertThat(apiJs).contains("getCookie('XSRF-TOKEN')");
        assertThat(apiJs).contains("headers['X-XSRF-TOKEN']");
        assertThat(apiJs).contains("async resolveRiotId(gameName, tagLine)");
    }

    private String readStaticFile(String relativePath) throws IOException {
        return Files.readString(STATIC_ROOT.resolve(relativePath));
    }
}
