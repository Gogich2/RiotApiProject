package org.main.util;

public final class SummonerNameNormalizer {

    private SummonerNameNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("summonerName is null");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("summonerName is blank");
        }

        // collapse multiple spaces
        trimmed = trimmed.replaceAll("\\s+", " ");

        if (trimmed.length() < 3 || trimmed.length() > 16) {
            throw new IllegalArgumentException("summonerName length must be 3..16");
        }

        return trimmed;
    }
}
