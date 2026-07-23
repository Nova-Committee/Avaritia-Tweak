package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftCodec;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftLoadResult;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftState;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftStore;
import committee.nova.mods.avaritia_tweak.customization.commit.CommitRequest;
import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;
import committee.nova.mods.avaritia_tweak.customization.commit.CustomizationCommitTarget;
import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceCodec;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.PreviewResult;
import committee.nova.mods.avaritia_tweak.customization.render.PreviewService;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;
import committee.nova.mods.avaritia_tweak.customization.render.WorkspaceRenderService;
import committee.nova.mods.avaritia_tweak.customization.validation.RegistryLookup;
import committee.nova.mods.avaritia_tweak.customization.validation.WorkspaceValidator;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class EditorControllerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void usesInjectedCommitTargetAndPreservesDraftSelectionAndPreviewOnFailure() {
        WorkspaceHead head = WorkspaceHead.initial();
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
        DraftState fallback = DraftState.committed(0, head.snapshot(), clock.instant());
        AtomicReference<CommitRequest> captured = new AtomicReference<>();
        CustomizationCommitTarget target = request -> {
            captured.set(request);
            return CompletableFuture.completedFuture(new CommitResult.IoFailure(
                    "test.failure", Optional.empty(), "injected failure"));
        };
        WorkspaceValidator validator = new WorkspaceValidator(new AcceptingRegistry());
        PreviewService previews = new PreviewService(validator, WorkspaceRenderService.standard());
        CustomizationHistory history = new EmptyHistory(head);

        try (DraftStore drafts = new DraftStore(this.temporaryDirectory.resolve("draft.json"),
                new DraftCodec(new WorkspaceCodec()))) {
            EditorController controller = new EditorController(head,
                    new DraftLoadResult.Missing(fallback), previews, target, history, drafts, clock);
            CustomizationEntry entry = new CustomizationEntry.Compressor(
                    id("test:controller"), OutputTarget.KUBEJS,
                    new IngredientSpec.Item(id("minecraft:stone")),
                    new ItemStackSpec(id("minecraft:diamond"), 1), 100, 20);
            controller.stage(entry, Optional.empty());
            PreviewResult preview = controller.preview();

            CommitResult result = controller.commit("keep my state").toCompletableFuture().join();

            assertThat(result).isInstanceOf(CommitResult.IoFailure.class);
            assertThat(captured.get()).isNotNull();
            assertThat(captured.get().snapshot()).isEqualTo(controller.draft());
            assertThat(captured.get().message()).isEqualTo("keep my state");
            assertThat(controller.committed()).isEqualTo(WorkspaceSnapshot.empty());
            assertThat(controller.draft().entries()).containsEntry(entry.key(), entry);
            assertThat(controller.selectedEntry()).contains(entry.key());
            assertThat(controller.lastPreview()).contains(preview);
            assertThat(controller.lastCommitResult()).contains(result);
        }
    }

    @Test
    void restoresOnlyTheRequestedEntryToTheCommittedSnapshot() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
        CustomizationEntry committed = compressor("test:restore", 100);
        CustomizationEntry untouched = compressor("test:untouched", 200);
        WorkspaceSnapshot committedSnapshot = WorkspaceSnapshot.empty()
                .withEntry(committed).withEntry(untouched);
        WorkspaceHead head = new WorkspaceHead(3, committedSnapshot, RenderPlan.of(List.of()));
        DraftState fallback = DraftState.committed(3, committedSnapshot, clock.instant());

        try (DraftStore drafts = new DraftStore(this.temporaryDirectory.resolve("restore-draft.json"),
                new DraftCodec(new WorkspaceCodec()))) {
            EditorController controller = new EditorController(head,
                    new DraftLoadResult.Missing(fallback),
                    new PreviewService(new WorkspaceValidator(new AcceptingRegistry()),
                            WorkspaceRenderService.standard()),
                    request -> CompletableFuture.completedFuture(new CommitResult.IoFailure(
                            "unused", Optional.empty(), "unused")),
                    new EmptyHistory(head), drafts, clock);
            CustomizationEntry modified = compressor("test:restore", 999);
            CustomizationEntry otherModified = compressor("test:untouched", 777);
            controller.stage(modified, Optional.of(committed.key()));
            controller.stage(otherModified, Optional.of(untouched.key()));
            controller.select(Optional.of(otherModified.key()));

            assertThat(controller.restoreEntry(committed.key())).isTrue();
            assertThat(controller.draft().entries()).containsEntry(committed.key(), committed);
            assertThat(controller.draft().entries()).containsEntry(otherModified.key(), otherModified);
            assertThat(controller.selectedEntry()).contains(otherModified.key());
            assertThat(controller.restoreEntry(committed.key())).isFalse();
        }
    }

    @Test
    void restoringAnAddedEntryRemovesItAndClearsItsSelection() {
        WorkspaceHead head = WorkspaceHead.initial();
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
        DraftState fallback = DraftState.committed(0, head.snapshot(), clock.instant());

        try (DraftStore drafts = new DraftStore(this.temporaryDirectory.resolve("added-draft.json"),
                new DraftCodec(new WorkspaceCodec()))) {
            EditorController controller = new EditorController(head,
                    new DraftLoadResult.Missing(fallback),
                    new PreviewService(new WorkspaceValidator(new AcceptingRegistry()),
                            WorkspaceRenderService.standard()),
                    request -> CompletableFuture.completedFuture(new CommitResult.IoFailure(
                            "unused", Optional.empty(), "unused")),
                    new EmptyHistory(head), drafts, clock);
            CustomizationEntry added = compressor("test:added", 100);
            controller.stage(added, Optional.empty());

            assertThat(controller.restoreEntry(added.key())).isTrue();
            assertThat(controller.draft().entries()).doesNotContainKey(added.key());
            assertThat(controller.selectedEntry()).isEmpty();
        }
    }

    private static CustomizationEntry compressor(String resourceId, int inputCount) {
        return new CustomizationEntry.Compressor(id(resourceId), OutputTarget.KUBEJS,
                new IngredientSpec.Item(id("minecraft:stone")),
                new ItemStackSpec(id("minecraft:diamond"), 1), inputCount, 20);
    }

    private static ResourceLocation id(String value) {
        return java.util.Objects.requireNonNull(ResourceLocation.tryParse(value));
    }

    private static final class AcceptingRegistry implements RegistryLookup {
        @Override
        public OptionalInt itemMaxStackSize(ResourceLocation itemId) {
            return OptionalInt.of(64);
        }

        @Override
        public boolean itemTagExists(ResourceLocation tagId) {
            return true;
        }
    }

    private record EmptyHistory(WorkspaceHead head) implements CustomizationHistory {
        @Override
        public WorkspaceHead loadHead() {
            return this.head;
        }

        @Override
        public List<Revision> loadHistory() {
            return List.of();
        }

        @Override
        public Revision loadRevision(long version) throws RepositoryException {
            throw new RepositoryException("test.missing", Path.of("history"),
                    "No revisions in test history");
        }
    }
}
