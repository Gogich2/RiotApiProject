package org.main.builds;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.main.builds.aggregate.BuildAggregator;
import org.main.builds.aggregate.ChampionBuildAggregationService;
import org.main.builds.aggregate.DefaultChampionBuildAggregationService;
import org.main.builds.extract.BuildObservationFactory;
import org.main.builds.extract.ItemSequenceExtractor;
import org.main.builds.extract.RunePageExtractor;
import org.main.builds.extract.SkillPathExtractor;
import org.main.builds.source.BuildSourceRepository;
import org.main.builds.source.ItemCatalog;
import org.main.builds.store.BuildPublisher;
import org.main.builds.store.BuildSnapshotRepository;
import org.main.builds.store.BuildSnapshotValidator;
import org.main.builds.store.JdbcBuildSnapshotRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BuildProperties.class)
public class BuildConfiguration {

    @Bean
    public BuildRules buildRules(BuildProperties properties) {
        if (properties.batchSize() <= 0) {
            throw new IllegalArgumentException("Build batch size must be positive");
        }
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

    @Bean
    public ItemSequenceExtractor itemSequenceExtractor(BuildProperties properties) {
        return new ItemSequenceExtractor(properties.startingItemsCutoff());
    }

    @Bean
    public RunePageExtractor runePageExtractor() {
        return new RunePageExtractor();
    }

    @Bean
    public SkillPathExtractor skillPathExtractor() {
        return new SkillPathExtractor();
    }

    @Bean
    public BuildObservationFactory buildObservationFactory(
            ItemSequenceExtractor itemSequenceExtractor,
            RunePageExtractor runePageExtractor,
            SkillPathExtractor skillPathExtractor,
            ItemCatalog itemCatalog
    ) {
        return new BuildObservationFactory(itemSequenceExtractor, runePageExtractor,
                skillPathExtractor, itemCatalog);
    }

    @Bean
    public BuildAggregator buildAggregator(BuildRules rules) {
        return new BuildAggregator(rules);
    }

    @Bean
    public BuildSnapshotValidator buildSnapshotValidator(BuildProperties properties) {
        return new BuildSnapshotValidator(properties.matchupMinGames());
    }

    @Bean
    public BuildSnapshotRepository buildSnapshotRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        return new JdbcBuildSnapshotRepository(jdbcTemplate, objectMapper, clock);
    }

    @Bean
    public BuildPublisher buildPublisher(
            BuildSnapshotRepository repository,
            BuildSnapshotValidator validator
    ) {
        return new BuildPublisher(repository, validator);
    }

    @Bean
    public ChampionBuildAggregationService championBuildAggregationService(
            BuildSourceRepository sourceRepository,
            ItemCatalog itemCatalog,
            BuildObservationFactory observationFactory,
            BuildAggregator aggregator,
            BuildSnapshotRepository snapshotRepository,
            BuildPublisher publisher,
            BuildProperties properties
    ) {
        return new DefaultChampionBuildAggregationService(sourceRepository, itemCatalog,
                observationFactory, aggregator, snapshotRepository, publisher, properties);
    }
}
