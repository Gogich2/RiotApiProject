package org.main.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StaticChampionBuildFrontendTest {

    private static final Path STATIC_ROOT = Path.of("src", "main", "resources", "static");

    @Test
    void championPageKeepsLegacyHooksAndAddsAccessibleBuildShell() throws IOException {
        String html = read("champion.html");

        int buildsIndex = html.indexOf("id=\"championBuilds\"");
        int itemsIndex = html.indexOf("id=\"championItemsBody\"");
        assertThat(buildsIndex).isGreaterThanOrEqualTo(0);
        assertThat(buildsIndex).isLessThan(itemsIndex);
        assertThat(html).contains("id=\"buildQueueTabs\"");
        assertThat(html).contains("id=\"buildPatchSelect\"");
        assertThat(html).contains("id=\"buildRoleTabs\"");
        assertThat(html).contains("for=\"buildOpponentSearch\"");
        assertThat(html).contains("id=\"buildOpponentSearch\"");
        assertThat(html).contains("id=\"buildOpponentRail\"");
        assertThat(html).contains("id=\"buildOpponentEmpty\"");
        assertThat(html).contains("id=\"buildEvidence\"");
        assertThat(html).contains("id=\"buildStartingItems\"");
        assertThat(html).contains("id=\"buildBootsCore\"");
        assertThat(html).contains("id=\"buildSituationalItems\"");
        assertThat(html).contains("id=\"buildRunes\"");
        assertThat(html).contains("id=\"buildSpells\"");
        assertThat(html).contains("id=\"buildSkillOrder\"");
        assertThat(html).contains("id=\"buildMaxPriority\"");
        assertThat(html).contains("id=\"buildStatus\"", "aria-live=\"polite\"");
        assertThat(html).contains("id=\"buildLoadingState\"");
        assertThat(html).contains("id=\"buildFallbackState\"");
        assertThat(html).contains("id=\"buildInsufficientState\"");
        assertThat(html).contains("id=\"buildUnavailableState\"");
        assertThat(html).contains("id=\"buildRequestError\"");
        assertThat(html).contains("id=\"buildRetryButton\"");
        assertThat(html).contains("js/champion-builds.js");
        assertThat(html).doesNotContain("role=\"tab\"", "role=\"tablist\"");
        int evidenceIndex = html.indexOf("id=\"buildEvidence\"");
        int situationalItemsIndex = html.indexOf("id=\"buildSituationalItems\"");
        assertThat(evidenceIndex).isGreaterThan(situationalItemsIndex);

        assertThat(html).contains("id=\"championHero\"");
        assertThat(html).contains("id=\"championStats\"");
        assertThat(html).contains("id=\"championAbilities\"");
        assertThat(html).contains("id=\"championItemsBody\"");
        assertThat(html).contains("Stored item statistics");
    }

    @Test
    void browserContractUsesPublicBuildApisAndExactUrlCacheDimensions() throws IOException {
        String api = read("js/api.js");
        String champion = read("js/champion.js");
        String builds = read("js/champion-builds.js");

        assertThat(api).contains("getChampionBuildOptions(championId");
        assertThat(api).contains("getChampionBuild(championId");
        assertThat(api).contains("queueId", "patch", "role", "opponentId");
        assertThat(champion).contains("api.getChampionItems(championId)");
        assertThat(champion).contains("championBuilds.mount");
        assertThat(builds).contains("'queue'", "'patch'", "'role'", "'opponent'");
        assertThat(builds).contains("history.replaceState");
        assertThat(builds).contains("championId", "queueId", "patch", "role", "opponentId");
        assertThat(builds).contains("sessionStorage");
        assertThat(builds).contains("All opponents", "aria-pressed");
        assertThat(builds).doesNotContain("—", "–", "·");
    }

    @Test
    void browserDoesNotOwnStatisticalDecisions() throws IOException {
        String builds = read("js/champion-builds.js");

        assertThat(builds).doesNotContain("0.70", "0.30", ".sort(");
        assertThat(builds).doesNotContain("matchupMinGames", "mediumConfidenceGames",
                "highConfidenceGames", "confidenceThreshold");
        assertThat(builds).doesNotMatch("(?s).*\\.wins\\s*/\\s*[^;\\n]*\\.games.*");
    }

    @Test
    void championBuildStylesCoverResponsiveAccessibleStates() throws IOException {
        String css = read("css/champion.css");

        assertThat(css).contains(
                ".champion-builds",
                ".build-filter-bar",
                ".build-queue-tabs",
                ".build-role-tabs",
                ".build-opponent-rail",
                ".build-evidence",
                ".build-component-grid",
                ".build-item-sequence",
                ".build-rune-page",
                ".build-skill-order",
                "#buildMaxPriority",
                ".build-state",
                ".build-state--stale",
                ".build-state--unavailable");
        assertThat(css).contains(
                "min-height: 44px", "overflow-x: auto", "align-content: start", ":focus-visible");
        assertThat(css).contains(
                "@media (min-width: 768px)",
                "@media (min-width: 1024px)",
                "@media (prefers-reduced-motion: reduce)");
        assertThat(css).doesNotContain("transition: all");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(STATIC_ROOT.resolve(relativePath));
    }
}
