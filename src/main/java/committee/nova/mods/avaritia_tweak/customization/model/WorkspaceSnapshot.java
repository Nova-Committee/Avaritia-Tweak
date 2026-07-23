package committee.nova.mods.avaritia_tweak.customization.model;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public record WorkspaceSnapshot(int schemaVersion, SortedMap<EntryKey, CustomizationEntry> entries) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public WorkspaceSnapshot {
        Objects.requireNonNull(entries, "entries");
        TreeMap<EntryKey, CustomizationEntry> copy = new TreeMap<>();
        entries.forEach((key, entry) -> {
            Objects.requireNonNull(key, "entry key");
            Objects.requireNonNull(entry, "entry");
            if (!key.equals(entry.key())) {
                throw new IllegalArgumentException("Workspace key does not match entry: " + key);
            }
            copy.put(key, entry);
        });
        entries = Collections.unmodifiableSortedMap(copy);
    }

    public static WorkspaceSnapshot empty() {
        return new WorkspaceSnapshot(CURRENT_SCHEMA_VERSION, new TreeMap<>());
    }

    public WorkspaceSnapshot withEntry(CustomizationEntry entry) {
        TreeMap<EntryKey, CustomizationEntry> copy = new TreeMap<>(this.entries);
        copy.put(entry.key(), entry);
        return new WorkspaceSnapshot(this.schemaVersion, copy);
    }

    public WorkspaceSnapshot withoutEntry(EntryKey key) {
        TreeMap<EntryKey, CustomizationEntry> copy = new TreeMap<>(this.entries);
        copy.remove(key);
        return new WorkspaceSnapshot(this.schemaVersion, copy);
    }
}
