package org.main.service.scheduler;

final class MaintenanceBudget {

    private static final int MATCH_ID_PAGE_SIZE = 20;

    private static final int MAX_CRAWLER_MATCHES = 100;

    private MaintenanceBudget() {
    }

    static int cycleBudget(int configuredLimit, int remainingCapacity, int headroom) {
        int safeHeadroom = Math.max(0, headroom);
        int configuredBudget = Math.max(0, configuredLimit - safeHeadroom);
        int liveBudget = Math.max(0, remainingCapacity - safeHeadroom);
        return Math.min(configuredBudget, liveBudget);
    }

    static int integrityProtectedBudget(int cycleBudget, int sharePercent) {
        int safeShare = Math.max(0, Math.min(100, sharePercent));
        return Math.max(0, cycleBudget) * safeShare / 100;
    }

    static int crawlerWorstCaseCost(int matchLimit) {
        if (matchLimit <= 0) {
            return 0;
        }

        int pageCalls = (matchLimit + MATCH_ID_PAGE_SIZE - 1) / MATCH_ID_PAGE_SIZE;
        return pageCalls + 2 * matchLimit;
    }

    static int maxCrawlerMatches(int requestBudget) {
        int matches = 0;

        while (matches < MAX_CRAWLER_MATCHES
                && crawlerWorstCaseCost(matches + 1) <= requestBudget) {
            matches++;
        }

        return matches;
    }
}
