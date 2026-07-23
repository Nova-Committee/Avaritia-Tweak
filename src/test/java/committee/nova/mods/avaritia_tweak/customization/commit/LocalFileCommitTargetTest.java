package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.ContentHashes;
import committee.nova.mods.avaritia_tweak.customization.render.KubeJsRenderer;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;
import committee.nova.mods.avaritia_tweak.customization.render.SingularityDatapackRenderer;
import committee.nova.mods.avaritia_tweak.customization.render.WorkspaceRenderService;
import committee.nova.mods.avaritia_tweak.customization.validation.RegistryLookup;
import committee.nova.mods.avaritia_tweak.customization.validation.WorkspaceValidator;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileCommitTargetTest {
    private static final Instant NOW = Instant.parse("2026-07-23T01:02:03Z");
    private final WorkspaceRenderService renderer = WorkspaceRenderService.standard();

    @TempDir
    Path temporaryDirectory;

    @Test
    void commitsWorkspaceArtifactsHistoryAndDraftAsOneVersion() throws Exception {
        LocalFileCommitTarget target = target(TransactionFaultInjector.NONE);
        WorkspaceSnapshot snapshot = singularity("test:first", OutputTarget.DATAPACK);
        RenderPlan preview = this.renderer.render(snapshot);

        CommitResult result = target.commitSync(CommitRequest.previewed(0, snapshot, "first", preview));

        assertThat(result).isInstanceOf(CommitResult.Success.class);
        Revision revision = ((CommitResult.Success) result).revision();
        assertThat(revision.version()).isEqualTo(1);
        assertThat(target.repository().loadHead().snapshot()).isEqualTo(snapshot);
        assertThat(target.repository().loadRevision(1)).isEqualTo(revision);
        assertThat(Files.isRegularFile(target.repository().draftFile())).isTrue();
        preview.artifacts().values().forEach(artifact -> {
            try {
                Path actual = this.temporaryDirectory.resolve("game").resolve(artifact.logicalPath().value());
                assertThat(Files.readAllBytes(actual)).containsExactly(artifact.utf8Content());
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
    }

    @Test
    void rejectsStaleVersionAndExternalChangesWithoutWritingAnythingElse() throws Exception {
        LocalFileCommitTarget target = target(TransactionFaultInjector.NONE);
        WorkspaceSnapshot first = WorkspaceSnapshot.empty();
        CommitResult initial = target.commitSync(CommitRequest.previewed(0, first, "initial",
                this.renderer.render(first)));
        assertThat(initial).isInstanceOf(CommitResult.Success.class);

        Path managedJs = this.temporaryDirectory.resolve("game").resolve(KubeJsRenderer.PATH.value());
        Files.writeString(managedJs, "external change\n", StandardCharsets.UTF_8);
        WorkspaceSnapshot second = singularity("test:second", OutputTarget.KUBEJS);

        CommitResult stale = target.commitSync(CommitRequest.previewed(0, second, "stale",
                this.renderer.render(second)));
        CommitResult conflict = target.commitSync(CommitRequest.previewed(1, second, "conflict",
                this.renderer.render(second)));

        assertThat(stale).isInstanceOf(CommitResult.VersionConflict.class);
        assertThat(conflict).isInstanceOf(CommitResult.ExternalFileConflict.class);
        assertThat(((CommitResult.ExternalFileConflict) conflict).conflicts())
                .extracting(FileConflict::logicalPath).contains(KubeJsRenderer.PATH);
        assertThat(target.repository().loadHead().version()).isEqualTo(1);
        assertThat(Files.readString(managedJs)).isEqualTo("external change\n");
    }

    @Test
    void explicitTakeoverBacksUpObservedExternalFileBeforeCommit() throws Exception {
        LocalFileCommitTarget target = target(TransactionFaultInjector.NONE);
        Path occupied = this.temporaryDirectory.resolve("game").resolve(KubeJsRenderer.PATH.value());
        Files.createDirectories(occupied.getParent());
        Files.writeString(occupied, "user content\n", StandardCharsets.UTF_8);
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.empty();
        CommitRequest request = CommitRequest.previewed(0, snapshot, "take over", this.renderer.render(snapshot));

        CommitResult conflictResult = target.commitSync(request);
        FileConflict conflict = ((CommitResult.ExternalFileConflict) conflictResult).conflicts().stream()
                .filter(value -> value.logicalPath().equals(KubeJsRenderer.PATH)).findFirst().orElseThrow();
        TreeMap<ArtifactPath, FileObservation> confirmations = new TreeMap<>();
        confirmations.put(conflict.logicalPath(), conflict.actual());
        CommitResult result = target.commitSync(request.withTakeover(confirmations));

        assertThat(result).isInstanceOf(CommitResult.Success.class);
        try (var backups = Files.walk(target.repository().externalBackupsDirectory())) {
            Path backup = backups.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("avaritia_tweak_generated.js"))
                    .findFirst().orElseThrow();
            assertThat(Files.readString(backup)).isEqualTo("user content\n");
        }
        assertThat(Files.readString(occupied)).isNotEqualTo("user content\n");
    }

    @Test
    void rejectsPreviewHashDriftBeforeWritingWorkspaceOrArtifacts() throws Exception {
        LocalFileCommitTarget target = target(TransactionFaultInjector.NONE);
        WorkspaceSnapshot snapshot = singularity("test:preview_drift", OutputTarget.KUBEJS);
        CommitRequest previewed = CommitRequest.previewed(0, snapshot, "stale preview",
                this.renderer.render(snapshot));
        TreeMap<ArtifactPath, String> staleHashes = new TreeMap<>(previewed.expectedArtifactSha256());
        staleHashes.put(KubeJsRenderer.PATH, "0".repeat(64));
        CommitRequest stale = new CommitRequest(previewed.baseVersion(), previewed.snapshot(),
                previewed.message(), staleHashes, OptionalLong.empty(), new TreeMap<>());

        CommitResult result = target.commitSync(stale);

        assertThat(result).isInstanceOf(CommitResult.PreviewMismatch.class);
        assertThat(target.repository().loadHead().version()).isZero();
        assertThat(Files.exists(target.repository().workspaceFile())).isFalse();
        assertThat(Files.exists(this.temporaryDirectory.resolve("game")
                .resolve(KubeJsRenderer.PATH.value()))).isFalse();
    }

    @Test
    void injectedFailureAtEveryReplacementRestoresEveryTargetAndLeavesHeadAtZero() throws Exception {
        WorkspaceSnapshot snapshot = singularity("test:failure", OutputTarget.DATAPACK);
        RenderPlan preview = this.renderer.render(snapshot);
        int mutationCount = preview.artifacts().size() + 3;

        for (int index = 0; index < mutationCount; index++) {
            int failureIndex = index;
            Path root = this.temporaryDirectory.resolve("replacement-" + index);
            LocalFileCommitTarget target = target(root, (actualIndex, path) -> {
                if (actualIndex == failureIndex) {
                    throw new java.io.IOException("injected failure at " + failureIndex);
                }
            });

            CommitResult result = target.commitSync(CommitRequest.previewed(0, snapshot, "will fail", preview));

            assertThat(result).as("failure index %s", failureIndex)
                    .isInstanceOf(CommitResult.IoFailure.class);
            assertThat(target.repository().loadHead().version()).isZero();
            for (ArtifactPath path : preview.artifacts().keySet()) {
                assertThat(Files.exists(root.resolve("game").resolve(path.value())))
                        .as("artifact %s after failure index %s", path, failureIndex).isFalse();
            }
            assertThat(Files.exists(target.repository().draftFile())).isFalse();
            assertThat(Files.exists(target.repository().workspaceFile())).isFalse();
            assertThat(Files.exists(target.repository().historyFile(1))).isFalse();
        }
    }

    @Test
    void rollbackCreatesNewVersionAndRestoresExactHistoricalArtifacts() throws Exception {
        LocalFileCommitTarget target = target(TransactionFaultInjector.NONE);
        WorkspaceSnapshot first = singularity("test:first", OutputTarget.DATAPACK);
        CommitResult.Success firstResult = (CommitResult.Success) target.commitSync(
                CommitRequest.previewed(0, first, "first", this.renderer.render(first)));
        WorkspaceSnapshot second = singularity("test:second", OutputTarget.KUBEJS);
        target.commitSync(CommitRequest.previewed(1, second, "second", this.renderer.render(second)));

        CommitRequest rollback = target.rollbackRequest(2, 1, "restore first");
        CommitResult result = target.commitSync(rollback);

        assertThat(result).isInstanceOf(CommitResult.Success.class);
        Revision rolledBack = ((CommitResult.Success) result).revision();
        assertThat(rolledBack.version()).isEqualTo(3);
        assertThat(rolledBack.rollbackOf()).hasValue(1);
        assertThat(rolledBack.snapshot()).isEqualTo(first);
        assertThat(rolledBack.artifacts()).isEqualTo(firstResult.revision().artifacts());
        assertThat(target.repository().loadHistory()).extracting(Revision::version)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void removingDatapackDefinitionDeletesOnlyPreviouslyManagedJson() throws Exception {
        LocalFileCommitTarget target = target(TransactionFaultInjector.NONE);
        WorkspaceSnapshot first = singularity("test:remove_me", OutputTarget.DATAPACK);
        target.commitSync(CommitRequest.previewed(0, first, "add", this.renderer.render(first)));
        ArtifactPath jsonPath = ArtifactPath.of(SingularityDatapackRenderer.ROOT
                + "data/test/singularities/remove_me.json");
        Path actualJson = this.temporaryDirectory.resolve("game").resolve(jsonPath.value());
        assertThat(Files.isRegularFile(actualJson)).isTrue();

        WorkspaceSnapshot empty = WorkspaceSnapshot.empty();
        CommitResult result = target.commitSync(CommitRequest.previewed(1, empty, "remove",
                this.renderer.render(empty)));

        assertThat(result).isInstanceOf(CommitResult.Success.class);
        assertThat(Files.exists(actualJson)).isFalse();
        assertThat(Files.isRegularFile(this.temporaryDirectory.resolve("game")
                .resolve(SingularityDatapackRenderer.PACK_METADATA.value()))).isTrue();
    }

    private LocalFileCommitTarget target(TransactionFaultInjector faultInjector) {
        return target(this.temporaryDirectory, faultInjector);
    }

    private LocalFileCommitTarget target(Path root, TransactionFaultInjector faultInjector) {
        return new LocalFileCommitTarget(root.resolve("game"),
                root.resolve("state"), new WorkspaceValidator(new TestRegistryLookup()),
                this.renderer, Clock.fixed(NOW, ZoneOffset.UTC), Runnable::run, faultInjector);
    }

    private static WorkspaceSnapshot singularity(String value, OutputTarget target) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        CustomizationEntry entry = new CustomizationEntry.SingularityDefinition(id, target,
                "singularity." + value, 0x112233, 0x445566, 1000, 240,
                new IngredientSpec.Item(ResourceLocation.tryParse("minecraft:stone")), true, true);
        return WorkspaceSnapshot.empty().withEntry(entry);
    }

    private static final class TestRegistryLookup implements RegistryLookup {
        @Override
        public OptionalInt itemMaxStackSize(ResourceLocation itemId) {
            return itemId.equals(ResourceLocation.tryParse("minecraft:stone"))
                    ? OptionalInt.of(64) : OptionalInt.empty();
        }

        @Override
        public boolean itemTagExists(ResourceLocation tagId) {
            return false;
        }
    }
}
