package committee.nova.mods.avaritia_tweak.customization.model;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record EntryKey(EntryKind kind, ResourceLocation id) implements Comparable<EntryKey> {
    public EntryKey {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
    }

    @Override
    public int compareTo(EntryKey other) {
        int kindComparison = this.kind.compareTo(other.kind);
        return kindComparison != 0 ? kindComparison : this.id.toString().compareTo(other.id.toString());
    }
}
