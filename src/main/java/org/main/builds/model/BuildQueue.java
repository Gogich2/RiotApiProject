package org.main.builds.model;

public enum BuildQueue {
    SOLO_DUO(420),
    FLEX(440);

    private final int id;

    BuildQueue(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static BuildQueue fromId(int queueId) {
        return switch (queueId) {
            case 420 -> SOLO_DUO;
            case 440 -> FLEX;
            default -> throw new IllegalArgumentException("Unsupported build queue: " + queueId);
        };
    }
}
