package org.main.builds.model;

import java.util.List;

public record SkillPath(List<Integer> order) {

    public SkillPath {
        order = List.copyOf(order);
    }
}
