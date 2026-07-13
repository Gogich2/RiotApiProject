package org.main.builds;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BuildProperties.class)
public class BuildConfiguration {

    @Bean
    public BuildRules buildRules(BuildProperties properties) {
        double anchorWeight = properties.anchorPatchWeight();
        double comparisonWeight = properties.comparisonPatchWeight();
        if (!Double.isFinite(anchorWeight) || !Double.isFinite(comparisonWeight)
                || anchorWeight < 0 || comparisonWeight < 0) {
            throw new IllegalArgumentException("Build patch weights must be finite and non-negative");
        }
        if (Math.abs(anchorWeight + comparisonWeight - 1.0) > 0.000000001) {
            throw new IllegalArgumentException("Build patch weights must sum to 1.0");
        }
        if (properties.matchupMinGames() < 0
                || properties.matchupMinGames() >= properties.mediumConfidenceGames()
                || properties.mediumConfidenceGames() >= properties.highConfidenceGames()) {
            throw new IllegalArgumentException("Build confidence thresholds must be ascending");
        }
        return new BuildRules(properties);
    }
}
