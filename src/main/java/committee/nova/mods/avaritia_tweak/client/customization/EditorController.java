package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftLoadResult;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftState;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftStore;
import committee.nova.mods.avaritia_tweak.customization.commit.CommitRequest;
import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;
import committee.nova.mods.avaritia_tweak.customization.commit.CustomizationCommitTarget;
import committee.nova.mods.avaritia_tweak.customization.commit.FileObservation;
import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.PreviewResult;
import committee.nova.mods.avaritia_tweak.customization.render.PreviewService;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;

public final class EditorController {
    private final PreviewService previewService;
    private final CustomizationCommitTarget commitTarget;
    private final CustomizationHistory history;
    private final DraftStore draftStore;
    private final Clock clock;
    private long baseVersion;
    private WorkspaceSnapshot committed;
    private WorkspaceSnapshot draft;
    private Optional<EntryKey> selectedEntry;
    private String activePanel;
    private Optional<PreviewResult> lastPreview = Optional.empty();
    private Optional<WorkspaceSnapshot> previewedSnapshot = Optional.empty();
    private Optional<CommitResult> lastCommitResult = Optional.empty();
    private Optional<String> draftWarning = Optional.empty();

    public EditorController(WorkspaceHead head, DraftLoadResult loadedDraft,
                            PreviewService previewService, CustomizationCommitTarget commitTarget,
                            CustomizationHistory history, DraftStore draftStore, Clock clock) {
        this.previewService = Objects.requireNonNull(previewService, "previewService");
        this.commitTarget = Objects.requireNonNull(commitTarget, "commitTarget");
        this.history = Objects.requireNonNull(history, "history");
        this.draftStore = Objects.requireNonNull(draftStore, "draftStore");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.baseVersion = head.version();
        this.committed = head.snapshot();
        final DraftState initial;
        if (loadedDraft instanceof DraftLoadResult.Loaded loaded) {
            initial = loaded.draft();
        } else if (loadedDraft instanceof DraftLoadResult.Missing missing) {
            initial = missing.fallback();
        } else {
            DraftLoadResult.Corrupt corrupt = (DraftLoadResult.Corrupt) loadedDraft;
            this.draftWarning = Optional.of("Corrupt draft preserved at " + corrupt.preservedCopy());
            initial = corrupt.fallback();
        }
        this.draft = initial.workspace();
        this.selectedEntry = initial.selectedEntry().filter(this.draft.entries()::containsKey);
        this.activePanel = initial.activePanel();
        if (initial.baseVersion() != head.version()) {
            this.draftWarning = Optional.of("Draft was based on version " + initial.baseVersion()
                    + "; changes are shown against current version " + head.version());
        }
    }

    public synchronized long baseVersion() {
        return this.baseVersion;
    }

    public synchronized WorkspaceSnapshot committed() {
        return this.committed;
    }

    public synchronized WorkspaceSnapshot draft() {
        return this.draft;
    }

    public synchronized Optional<EntryKey> selectedEntry() {
        return this.selectedEntry;
    }

    public synchronized void select(Optional<EntryKey> key) {
        this.selectedEntry = key.filter(this.draft.entries()::containsKey);
        scheduleDraft();
    }

    public synchronized void stage(CustomizationEntry entry, Optional<EntryKey> replacing) {
        WorkspaceSnapshot updated = this.draft;
        if (replacing.isPresent() && !replacing.get().equals(entry.key())) {
            updated = updated.withoutEntry(replacing.get());
        }
        this.draft = updated.withEntry(entry);
        this.selectedEntry = Optional.of(entry.key());
        invalidatePreview();
        scheduleDraft();
    }

    public synchronized void remove(EntryKey key) {
        this.draft = this.draft.withoutEntry(key);
        if (this.selectedEntry.filter(key::equals).isPresent()) {
            this.selectedEntry = Optional.empty();
        }
        invalidatePreview();
        scheduleDraft();
    }

    /** Restores one draft entry to the committed snapshot without disturbing other draft entries. */
    public synchronized boolean restoreEntry(EntryKey key) {
        Objects.requireNonNull(key, "key");
        CustomizationEntry committedEntry = this.committed.entries().get(key);
        WorkspaceSnapshot restored = committedEntry == null
                ? this.draft.withoutEntry(key)
                : this.draft.withEntry(committedEntry);
        if (restored.equals(this.draft)) {
            return false;
        }
        this.draft = restored;
        if (committedEntry == null && this.selectedEntry.filter(key::equals).isPresent()) {
            this.selectedEntry = Optional.empty();
        }
        invalidatePreview();
        scheduleDraft();
        return true;
    }

    public synchronized void clearDraft() {
        this.draft = WorkspaceSnapshot.empty();
        this.selectedEntry = Optional.empty();
        invalidatePreview();
        scheduleDraft();
    }

    public synchronized void discardChanges() {
        this.draft = this.committed;
        this.selectedEntry = Optional.empty();
        invalidatePreview();
        scheduleDraft();
    }

    public synchronized PreviewResult preview() {
        PreviewResult preview = this.previewService.preview(this.committed, this.draft);
        this.lastPreview = Optional.of(preview);
        this.previewedSnapshot = preview.renderPlan().isPresent()
                ? Optional.of(this.draft)
                : Optional.empty();
        return preview;
    }

    public CompletionStage<CommitResult> commit(String message) {
        return commit(message, new TreeMap<>());
    }

    public CompletionStage<CommitResult> commit(
            String message, SortedMap<ArtifactPath, FileObservation> takeoverConfirmations) {
        final CommitRequest request;
        synchronized (this) {
            PreviewResult preview = ensureCurrentPreview();
            if (preview.renderPlan().isEmpty()) {
                CommitResult failure = new CommitResult.ValidationFailed(preview.validation());
                this.lastCommitResult = Optional.of(failure);
                return java.util.concurrent.CompletableFuture.completedFuture(failure);
            }
            request = CommitRequest.previewed(this.baseVersion, this.draft, message,
                    preview.renderPlan().orElseThrow()).withTakeover(takeoverConfirmations);
        }
        return this.commitTarget.commit(request).thenApply(this::acceptCommitResult);
    }

    public CompletionStage<CommitResult> rollback(Revision revision, String message) {
        return rollback(revision, message, new TreeMap<>());
    }

    public CompletionStage<CommitResult> rollback(
            Revision revision, String message,
            SortedMap<ArtifactPath, FileObservation> takeoverConfirmations) {
        TreeMap<ArtifactPath, String> hashes = new TreeMap<>();
        revision.artifacts().artifacts().forEach((path, artifact) -> hashes.put(path, artifact.sha256()));
        final CommitRequest request;
        synchronized (this) {
            request = new CommitRequest(this.baseVersion, revision.snapshot(), message, hashes,
                    OptionalLong.of(revision.version()), takeoverConfirmations);
        }
        return this.commitTarget.commit(request).thenApply(this::acceptCommitResult);
    }

    public synchronized List<Revision> history() throws RepositoryException {
        return this.history.loadHistory();
    }

    public synchronized Revision revision(long version) throws RepositoryException {
        return this.history.loadRevision(version);
    }

    public synchronized Optional<PreviewResult> lastPreview() {
        return this.lastPreview;
    }

    public synchronized Optional<CommitResult> lastCommitResult() {
        return this.lastCommitResult;
    }

    public synchronized Optional<String> draftWarning() {
        return this.draftWarning;
    }

    public synchronized String activePanel() {
        return this.activePanel;
    }

    public synchronized void activePanel(String activePanel) {
        this.activePanel = activePanel;
        scheduleDraft();
    }

    public synchronized void flushDraft() {
        this.draftStore.flush();
    }

    private synchronized CommitResult acceptCommitResult(CommitResult result) {
        this.lastCommitResult = Optional.of(result);
        if (result instanceof CommitResult.Success success) {
            Revision revision = success.revision();
            this.baseVersion = revision.version();
            this.committed = revision.snapshot();
            this.draft = revision.snapshot();
            this.selectedEntry = this.selectedEntry.filter(this.draft.entries()::containsKey);
            this.draftWarning = Optional.empty();
            invalidatePreview();
            scheduleDraft();
        }
        return result;
    }

    private PreviewResult ensureCurrentPreview() {
        if (this.lastPreview.isEmpty() || this.previewedSnapshot.filter(this.draft::equals).isEmpty()) {
            return preview();
        }
        return this.lastPreview.orElseThrow();
    }

    private void invalidatePreview() {
        this.lastPreview = Optional.empty();
        this.previewedSnapshot = Optional.empty();
    }

    private void scheduleDraft() {
        this.draftStore.schedule(new DraftState(DraftState.CURRENT_SCHEMA_VERSION, this.baseVersion,
                this.clock.instant(), this.draft, this.selectedEntry, this.activePanel));
    }
}
