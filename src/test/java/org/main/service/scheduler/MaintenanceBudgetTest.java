package org.main.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaintenanceBudgetTest {

    @Test
    void leavesHeadroomAndUsesTheSmallerLiveCapacity() {
        assertThat(MaintenanceBudget.cycleBudget(85, 85, 1)).isEqualTo(84);
        assertThat(MaintenanceBudget.cycleBudget(85, 40, 1)).isEqualTo(39);
        assertThat(MaintenanceBudget.cycleBudget(85, 0, 1)).isZero();
    }

    @Test
    void protectsHalfForIntegrity() {
        assertThat(MaintenanceBudget.integrityProtectedBudget(84, 50)).isEqualTo(42);
        assertThat(MaintenanceBudget.integrityProtectedBudget(39, 50)).isEqualTo(19);
    }

    @Test
    void includesMatchIdPaginationInWorstCaseCrawlerCost() {
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(20)).isEqualTo(41);
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(21)).isEqualTo(44);
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(40)).isEqualTo(82);
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(41)).isEqualTo(85);
    }

    @Test
    void convertsRequestBudgetToTheLargestSafeMatchLimit() {
        assertThat(MaintenanceBudget.maxCrawlerMatches(42)).isEqualTo(20);
        assertThat(MaintenanceBudget.maxCrawlerMatches(84)).isEqualTo(40);
        assertThat(MaintenanceBudget.maxCrawlerMatches(2)).isZero();
    }
}
