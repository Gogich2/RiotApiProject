package org.main.builds.api;

import org.main.builds.model.BuildRole;

public record RoleOption(BuildRole role, int games, boolean available) {
}
