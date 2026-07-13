package org.main.builds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.main.builds.model.BuildCandidate;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.PatchVersion;
import org.main.builds.model.PatchWindow;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BuildRulesTest {

    private final BuildRules rules = new BuildConfiguration().buildRules(properties());

    @ParameterizedTest
    @CsvSource({
            "0, INSUFFICIENT",
            "9, INSUFFICIENT",
            "10, LOW",
            "24, LOW",
            "25, MEDIUM",
            "49, MEDIUM",
            "50, HIGH"
    })
    void classifiesConfidenceAtEveryBoundary(int games, BuildConfidence expected) {
        assertThat(rules.confidence(games)).isEqualTo(expected);
    }

    @Test
    void exactMatchupsStartAtTheMinimumSample() {
        assertThat(rules.exactMatchupEligible(9)).isFalse();
        assertThat(rules.exactMatchupEligible(10)).isTrue();
    }

    @Test
    void acceptsOnlySupportedQueues() {
        assertThat(BuildQueue.fromId(420)).isEqualTo(BuildQueue.SOLO_DUO);
        assertThat(BuildQueue.fromId(440)).isEqualTo(BuildQueue.FLEX);
        assertThatThrownBy(() -> BuildQueue.fromId(430)).
                isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "TOP, TOP",
            "JUNGLE, JUNGLE",
            "MIDDLE, MIDDLE",
            "BOTTOM, BOTTOM",
            "UTILITY, UTILITY"
    })
    void acceptsCanonicalTeamPositions(String position, BuildRole expected) {
        assertThat(BuildRole.fromParticipant(position, "TOP")).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "TOP, TOP",
            "JUNGLE, JUNGLE",
            "MIDDLE, MIDDLE",
            "BOTTOM, BOTTOM",
            "UTILITY, UTILITY"
    })
    void fallsBackToCanonicalIndividualPositionWhenTeamPositionIsBlank(
            String position, BuildRole expected
    ) {
        assertThat(BuildRole.fromParticipant(" ", position)).isEqualTo(expected);
    }

    @Test
    void teamPositionTakesPriorityOverIndividualPosition() {
        assertThat(BuildRole.fromParticipant("TOP", "JUNGLE")).isEqualTo(BuildRole.TOP);
    }

    @Test
    void rejectsAliasesBlankAndUnknownRolesInsteadOfGuessing() {
        assertThatThrownBy(() -> BuildRole.fromParticipant("MID", "MIDDLE")).
                isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BuildRole.fromParticipant("", "")).
                isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BuildRole.fromParticipant(null, "CARRY")).
                isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesOnlySnapshotScopesOwnedByThisFeature() {
        assertThat(BuildScope.values()).
                containsExactly(BuildScope.CHAMPION_ROLE, BuildScope.EXACT_MATCHUP);
    }

    @Test
    void patchVersionsSortNumerically() {
        assertThat(PatchVersion.parse("16.10")).
                isGreaterThan(PatchVersion.parse("16.9"));
    }

    @Test
    void patchVersionsDiscardBuildComponents() {
        assertThat(PatchVersion.parse("16.9.123.456").displayName()).isEqualTo("16.9");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "16", "v16.9", "16.x", "16.9beta", "16.9."})
    void rejectsMalformedPatchVersions(String version) {
        assertThatThrownBy(() -> PatchVersion.parse(version)).
                isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void minorPatchWindowUsesTheExactAdjacentPatchEvenWhenItIsNotStored() {
        PatchWindow window = PatchWindow.from(
                PatchVersion.parse("16.9"),
                List.of(PatchVersion.parse("16.7"), PatchVersion.parse("16.9"))
        );

        assertThat(window).isEqualTo(new PatchWindow("16.9", "16.8"));
    }

    @Test
    void annualPatchWindowUsesTheHighestStoredPatchFromThePreviousMajor() {
        PatchWindow window = PatchWindow.from(
                PatchVersion.parse("16.1"),
                List.of(
                        PatchVersion.parse("14.24"),
                        PatchVersion.parse("15.7"),
                        PatchVersion.parse("15.24"),
                        PatchVersion.parse("16.1")
                )
        );

        assertThat(window).isEqualTo(new PatchWindow("16.1", "15.24"));
    }

    @Test
    void annualPatchWindowRejectsMissingPreviousMajorData() {
        assertThatThrownBy(() -> PatchWindow.from(
                PatchVersion.parse("16.1"),
                List.of(PatchVersion.parse("14.24"), PatchVersion.parse("16.1"))
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void weightedScoreUsesConfiguredPatchWeightsRatherThanRawPicks() {
        BuildCandidate<Integer> candidate = new BuildCandidate<>(1, 7, 10, 5, 6);

        assertThat(rules.weightedPickScore(candidate)).isCloseTo(7.9, within(0.000000001));
        assertThat(rules.weightedWinRate(candidate)).
                isCloseTo(5.3 / 7.9, within(0.000000001));
    }

    @Test
    void zeroPickCandidateHasZeroWeightedWinRate() {
        assertThat(rules.weightedWinRate(new BuildCandidate<>(1, 0, 0, 0, 0))).
                isZero();
    }

    @Test
    void ranksNumericIdsByScoreThenWinRateThenAscendingId() {
        BuildCandidate<Integer> highestScore = new BuildCandidate<>(4, 11, 0, 0, 0);
        BuildCandidate<Integer> lowerWinRate = new BuildCandidate<>(3, 10, 0, 5, 0);
        BuildCandidate<Integer> higherId = new BuildCandidate<>(2, 10, 0, 6, 0);
        BuildCandidate<Integer> lowerId = new BuildCandidate<>(1, 10, 0, 6, 0);

        assertThat(rules.rank(List.of(lowerWinRate, higherId, highestScore, lowerId))).
                extracting(BuildCandidate::value).
                containsExactly(4, 1, 2, 3);
    }

    @Test
    void ranksStringIdsAscendingAsTheFinalTieBreaker() {
        BuildCandidate<String> zeta = new BuildCandidate<>("zeta", 10, 0, 6, 0);
        BuildCandidate<String> alpha = new BuildCandidate<>("alpha", 10, 0, 6, 0);

        assertThat(rules.rank(List.of(zeta, alpha))).
                extracting(BuildCandidate::value).
                containsExactly("alpha", "zeta");
    }

    @Test
    void bindsBuildPropertiesUnderTheCompactPrefix() {
        ConfigurationProperties annotation = BuildProperties.class.
                getAnnotation(ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("app.builds");
    }

    @Test
    void rejectsWeightsThatDoNotSumToOne() {
        BuildProperties invalid = properties(10, 25, 50, 0.8, 0.3);

        assertThatThrownBy(() -> new BuildConfiguration().buildRules(invalid)).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("weights");
    }

    @ParameterizedTest
    @MethodSource("invalidPatchWeights")
    void rejectsNonFiniteAndNegativeWeightsBeforeCheckingTheirSum(
            double anchorWeight,
            double comparisonWeight
    ) {
        BuildProperties invalid = properties(10, 25, 50, anchorWeight, comparisonWeight);

        assertThatThrownBy(() -> new BuildConfiguration().buildRules(invalid)).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("finite and non-negative");
    }

    @Test
    void rejectsConfidenceThresholdsThatAreNotAscending() {
        BuildProperties invalid = properties(10, 10, 50, 0.7, 0.3);

        assertThatThrownBy(() -> new BuildConfiguration().buildRules(invalid)).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("thresholds");
    }

    private static BuildProperties properties() {
        return properties(10, 25, 50, 0.7, 0.3);
    }

    private static Stream<Arguments> invalidPatchWeights() {
        return Stream.of(
                Arguments.of(Double.NaN, 0.3),
                Arguments.of(Double.POSITIVE_INFINITY, 0.3),
                Arguments.of(Double.NEGATIVE_INFINITY, 0.3),
                Arguments.of(0.7, Double.NaN),
                Arguments.of(0.7, Double.POSITIVE_INFINITY),
                Arguments.of(0.7, Double.NEGATIVE_INFINITY),
                Arguments.of(-0.1, 1.1),
                Arguments.of(1.1, -0.1)
        );
    }

    private static BuildProperties properties(
            int matchupMinGames,
            int mediumConfidenceGames,
            int highConfidenceGames,
            double anchorPatchWeight,
            double comparisonPatchWeight
    ) {
        return new BuildProperties(
                1,
                1,
                matchupMinGames,
                mediumConfidenceGames,
                highConfidenceGames,
                anchorPatchWeight,
                comparisonPatchWeight,
                2,
                Duration.ofMinutes(2),
                250,
                Duration.ofHours(1),
                false
        );
    }
}
