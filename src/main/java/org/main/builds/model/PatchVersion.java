package org.main.builds.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PatchVersion(int major, int minor) implements Comparable<PatchVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.\\d+)*$");

    public PatchVersion {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("Patch components must be non-negative");
        }
    }

    public static PatchVersion parse(String gameVersion) {
        Matcher matcher = VERSION_PATTERN.matcher(gameVersion == null ? "" : gameVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed patch version: " + gameVersion);
        }
        return new PatchVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
        );
    }

    public String displayName() {
        return major + "." + minor;
    }

    @Override
    public int compareTo(PatchVersion other) {
        int majorComparison = Integer.compare(major, other.major);
        return majorComparison != 0 ? majorComparison : Integer.compare(minor, other.minor);
    }
}
