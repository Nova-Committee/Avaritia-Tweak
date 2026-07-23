package committee.nova.mods.avaritia_tweak.client.customization.persistence;

import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record DraftState(int schemaVersion, long baseVersion, Instant savedAt,
                         WorkspaceSnapshot workspace, Optional<EntryKey> selectedEntry,
                         String activePanel) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public DraftState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported draft schema " + schemaVersion);
        }
        if (baseVersion < 0) {
            throw new IllegalArgumentException("Draft base version cannot be negative");
        }
        Objects.requireNonNull(savedAt, "savedAt");
        Objects.requireNonNull(workspace, "workspace");
        selectedEntry = Objects.requireNonNull(selectedEntry, "selectedEntry");
        Objects.requireNonNull(activePanel, "activePanel");
    }

    public static DraftState committed(long version, WorkspaceSnapshot workspace, Instant savedAt) {
        return new DraftState(CURRENT_SCHEMA_VERSION, version, savedAt, workspace,
                Optional.empty(), "editor");
    }
}
