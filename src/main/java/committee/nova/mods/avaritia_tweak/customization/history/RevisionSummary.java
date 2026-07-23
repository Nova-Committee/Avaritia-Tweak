package committee.nova.mods.avaritia_tweak.customization.history;

import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiff;

public record RevisionSummary(int added, int modified, int removed) {
    public RevisionSummary {
        if (added < 0 || modified < 0 || removed < 0) {
            throw new IllegalArgumentException("Revision counts cannot be negative");
        }
    }

    public static RevisionSummary from(WorkspaceDiff diff) {
        return new RevisionSummary(
                Math.toIntExact(diff.count(ChangeType.ADDED)),
                Math.toIntExact(diff.count(ChangeType.MODIFIED)),
                Math.toIntExact(diff.count(ChangeType.REMOVED)));
    }
}
