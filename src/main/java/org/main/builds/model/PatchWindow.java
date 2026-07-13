package org.main.builds.model;

import java.util.Collection;

public record PatchWindow(String anchorPatch, String comparisonPatch) {

    public static PatchWindow forAnchor(
            PatchVersion anchor,
            Collection<PatchVersion> storedPatches
    ) {
        PatchVersion comparison;
        if (anchor.minor() > 1) {
            comparison = new PatchVersion(anchor.major(), anchor.minor() - 1);
        } else {
            comparison = storedPatches.stream().
                    filter(patch -> patch.major() == anchor.major() - 1).
                    max(PatchVersion::compareTo).
                    orElseThrow(() -> new IllegalArgumentException(
                            "No stored patch exists for previous major " + (anchor.major() - 1)
                    ));
        }
        return new PatchWindow(anchor.displayName(), comparison.displayName());
    }

    public static PatchWindow from(PatchVersion anchor, Collection<PatchVersion> storedPatches) {
        return forAnchor(anchor, storedPatches);
    }
}
