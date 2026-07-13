package org.main.builds.model;

public enum BuildRole {
    TOP,
    JUNGLE,
    MIDDLE,
    BOTTOM,
    UTILITY;

    public static BuildRole fromParticipant(String teamPosition, String individualPosition) {
        String position = teamPosition == null || teamPosition.isBlank()
                ? individualPosition
                : teamPosition;
        try {
            return BuildRole.valueOf(position == null ? "" : position);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported build role: " + position, exception);
        }
    }
}
