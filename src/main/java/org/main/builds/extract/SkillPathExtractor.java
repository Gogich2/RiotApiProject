package org.main.builds.extract;

import java.util.List;
import org.main.builds.model.SkillPath;

public final class SkillPathExtractor {

    public SkillPath extract(List<Integer> skills) {
        if (skills.isEmpty() || skills.stream().anyMatch(slot -> slot == null || slot < 1 || slot > 4)) {
            throw new IllegalArgumentException("Skill order is invalid");
        }
        return new SkillPath(skills);
    }
}
