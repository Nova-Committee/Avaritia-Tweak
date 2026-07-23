package committee.nova.mods.avaritia_tweak.customization.diff;

import java.util.List;
import java.util.Objects;

public record WorkspaceDiff(List<EntryChange> entries) {
    public WorkspaceDiff {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public long count(ChangeType type) {
        return this.entries.stream().filter(entry -> entry.type() == type).count();
    }
}
