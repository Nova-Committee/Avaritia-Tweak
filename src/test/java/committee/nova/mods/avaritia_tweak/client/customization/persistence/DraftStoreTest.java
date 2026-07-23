package committee.nova.mods.avaritia_tweak.client.customization.persistence;

import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceCodec;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DraftStoreTest {
    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void flushesPendingDraftAtomicallyAndLoadsIt() {
        Path draftFile = this.temporaryDirectory.resolve("draft.json");
        DraftCodec codec = new DraftCodec(new WorkspaceCodec());
        DraftState draft = DraftState.committed(3, WorkspaceSnapshot.empty(), NOW);

        try (DraftStore store = new DraftStore(draftFile, codec,
                Clock.fixed(NOW, ZoneOffset.UTC), 60_000)) {
            store.schedule(draft);
            assertThat(store.flush()).isInstanceOf(DraftSaveResult.Success.class);
            DraftLoadResult loaded = store.load(new WorkspaceHead(0, WorkspaceSnapshot.empty(),
                    RenderPlan.of(List.of())));
            assertThat(loaded).isInstanceOf(DraftLoadResult.Loaded.class);
            assertThat(((DraftLoadResult.Loaded) loaded).draft()).isEqualTo(draft);
        }
    }

    @Test
    void preservesCorruptDraftAndFallsBackToCommittedWorkspace() throws Exception {
        Path draftFile = this.temporaryDirectory.resolve("draft.json");
        Files.writeString(draftFile, "not-json", StandardCharsets.UTF_8);
        WorkspaceHead fallback = new WorkspaceHead(7, WorkspaceSnapshot.empty(), RenderPlan.of(List.of()));

        try (DraftStore store = new DraftStore(draftFile, new DraftCodec(new WorkspaceCodec()),
                Clock.fixed(NOW, ZoneOffset.UTC), 0)) {
            DraftLoadResult result = store.load(fallback);

            assertThat(result).isInstanceOf(DraftLoadResult.Corrupt.class);
            DraftLoadResult.Corrupt corrupt = (DraftLoadResult.Corrupt) result;
            assertThat(corrupt.fallback().baseVersion()).isEqualTo(7);
            assertThat(Files.readString(corrupt.preservedCopy())).isEqualTo("not-json");
            assertThat(Files.readString(draftFile)).isEqualTo("not-json");
        }
    }
}
