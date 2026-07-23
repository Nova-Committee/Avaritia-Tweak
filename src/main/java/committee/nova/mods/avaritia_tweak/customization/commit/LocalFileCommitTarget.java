package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftCodec;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftState;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiffer;
import committee.nova.mods.avaritia_tweak.customization.history.LocalWorkspaceRepository;
import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.history.RevisionCodec;
import committee.nova.mods.avaritia_tweak.customization.history.RevisionSummary;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceCodec;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.ContentHashes;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;
import committee.nova.mods.avaritia_tweak.customization.render.WorkspaceRenderService;
import committee.nova.mods.avaritia_tweak.customization.validation.ValidationIssue;
import committee.nova.mods.avaritia_tweak.customization.validation.ValidationReport;
import committee.nova.mods.avaritia_tweak.customization.validation.WorkspaceValidator;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class LocalFileCommitTarget implements CustomizationCommitTarget {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private final WorkspaceValidator validator;
    private final WorkspaceRenderService renderer;
    private final WorkspaceDiffer differ;
    private final WorkspaceCodec workspaceCodec;
    private final DraftCodec draftCodec;
    private final LocalWorkspaceRepository repository;
    private final ManagedPathResolver resolver;
    private final FileTransaction transaction;
    private final Clock clock;
    private final Executor executor;

    public LocalFileCommitTarget(Path gameRoot, Path stateRoot, WorkspaceValidator validator,
                                 WorkspaceRenderService renderer) {
        this(gameRoot, stateRoot, validator, renderer, Clock.systemUTC(),
                ForkJoinPool.commonPool(), TransactionFaultInjector.NONE);
    }

    public LocalFileCommitTarget(Path gameRoot, Path stateRoot, WorkspaceValidator validator,
                                 WorkspaceRenderService renderer, Clock clock, Executor executor,
                                 TransactionFaultInjector faultInjector) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.differ = new WorkspaceDiffer();
        this.workspaceCodec = new WorkspaceCodec();
        this.draftCodec = new DraftCodec(this.workspaceCodec);
        RevisionCodec revisionCodec = new RevisionCodec(this.workspaceCodec);
        this.repository = new LocalWorkspaceRepository(stateRoot, revisionCodec);
        this.resolver = new ManagedPathResolver(gameRoot, stateRoot);
        this.transaction = new FileTransaction(this.resolver, this.repository.transactionsDirectory(),
                Objects.requireNonNull(faultInjector, "faultInjector"));
        this.clock = Objects.requireNonNull(clock, "clock");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletionStage<CommitResult> commit(CommitRequest request) {
        Objects.requireNonNull(request, "request");
        return CompletableFuture.supplyAsync(() -> commitSync(request), this.executor);
    }

    public CommitResult commitSync(CommitRequest request) {
        try {
            Files.createDirectories(this.repository.stateRoot());
        } catch (IOException exception) {
            return ioFailure("state.root.create", null, exception);
        }
        try (FileChannel channel = FileChannel.open(this.repository.lockFile(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = tryLock(channel)) {
            if (ignored == null) {
                return new CommitResult.TargetBusy("Another visual-editor commit is in progress");
            }
            try {
                this.transaction.recoverPending();
            } catch (FileTransactionException exception) {
                return new CommitResult.RecoveryFailure(exception.getMessage());
            }
            return commitLocked(request);
        } catch (OverlappingFileLockException exception) {
            return new CommitResult.TargetBusy("Another visual-editor commit is in progress");
        } catch (IOException exception) {
            return ioFailure("commit.lock.failed", null, exception);
        }
    }

    public CommitRequest rollbackRequest(long baseVersion, long targetVersion, String message)
            throws RepositoryException {
        Revision target = this.repository.loadRevision(targetVersion);
        TreeMap<ArtifactPath, String> hashes = hashes(target.artifacts());
        return new CommitRequest(baseVersion, target.snapshot(), message, hashes,
                OptionalLong.of(targetVersion), new TreeMap<>());
    }

    public LocalWorkspaceRepository repository() {
        return this.repository;
    }

    /** Returns empty on successful recovery, otherwise the actionable failure. */
    public Optional<CommitResult> recoverPendingTransactions() {
        try {
            Files.createDirectories(this.repository.stateRoot());
            try (FileChannel channel = FileChannel.open(this.repository.lockFile(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = tryLock(channel)) {
                if (ignored == null) {
                    return Optional.of(new CommitResult.TargetBusy(
                            "Another visual-editor commit is in progress"));
                }
                try {
                    this.transaction.recoverPending();
                    return Optional.empty();
                } catch (FileTransactionException exception) {
                    return Optional.of(new CommitResult.RecoveryFailure(exception.getMessage()));
                }
            }
        } catch (IOException exception) {
            return Optional.of(ioFailure("recovery.lock.failed", null, exception));
        }
    }

    private CommitResult commitLocked(CommitRequest request) {
        final WorkspaceHead head;
        try {
            head = this.repository.loadHead();
        } catch (RepositoryException exception) {
            return new CommitResult.IoFailure(exception.code(), Optional.empty(), exception.getMessage());
        }
        if (request.baseVersion() != head.version()) {
            return new CommitResult.VersionConflict(request.baseVersion(), head.version());
        }

        ValidationReport validation = validateRequest(request);
        if (!validation.isValid()) {
            return new CommitResult.ValidationFailed(validation);
        }

        final RenderPlan newPlan;
        if (request.rollbackOf().isPresent()) {
            long targetVersion = request.rollbackOf().getAsLong();
            final Revision target;
            try {
                target = this.repository.loadRevision(targetVersion);
            } catch (RepositoryException exception) {
                return new CommitResult.IoFailure(exception.code(), Optional.empty(), exception.getMessage());
            }
            if (!target.snapshot().equals(request.snapshot())) {
                return new CommitResult.IoFailure("rollback.snapshot.mismatch", Optional.empty(),
                        "Rollback request does not match the selected historical snapshot");
            }
            newPlan = target.artifacts();
        } else {
            try {
                newPlan = this.renderer.render(request.snapshot());
            } catch (RuntimeException exception) {
                return ioFailure("render.failed", null, exception);
            }
        }

        TreeMap<ArtifactPath, String> actualHashes = hashes(newPlan);
        if (!actualHashes.equals(request.expectedArtifactSha256())) {
            return new CommitResult.PreviewMismatch(request.expectedArtifactSha256(), actualHashes);
        }

        List<FileConflict> conflicts;
        try {
            conflicts = detectConflicts(head, newPlan, request.takeoverConfirmations());
        } catch (IOException exception) {
            return ioFailure("conflict.check.failed", null, exception);
        }
        if (!conflicts.isEmpty()) {
            return new CommitResult.ExternalFileConflict(conflicts);
        }

        long newVersion = head.version() + 1;
        List<ArtifactPath> deletedPaths = head.artifacts().artifacts().keySet().stream()
                .filter(path -> !newPlan.artifacts().containsKey(path))
                .toList();
        Revision revision = new Revision(newVersion, head.version(), this.clock.instant(),
                request.message().strip(),
                RevisionSummary.from(this.differ.diff(head.snapshot(), request.snapshot())),
                request.rollbackOf(), request.snapshot(),
                ContentHashes.sha256(this.workspaceCodec.encode(request.snapshot())),
                newPlan, deletedPaths);

        final List<TransactionMutation> mutations;
        try {
            mutations = mutations(revision, head, request.takeoverConfirmations());
            List<FileConflict> secondCheck = detectConflicts(head, newPlan, request.takeoverConfirmations());
            if (!secondCheck.isEmpty()) {
                return new CommitResult.ExternalFileConflict(secondCheck);
            }
        } catch (IOException exception) {
            return ioFailure("commit.prepare.failed", null, exception);
        }

        try {
            this.transaction.apply(mutations);
        } catch (FileTransactionException exception) {
            return exception.recoveryFailure()
                    ? new CommitResult.RecoveryFailure(exception.getMessage())
                    : new CommitResult.IoFailure("commit.transaction.failed", Optional.empty(),
                    exception.getMessage());
        }
        return new CommitResult.Success(revision);
    }

    private ValidationReport validateRequest(CommitRequest request) {
        ValidationReport base = this.validator.validate(request.snapshot());
        List<ValidationIssue> issues = new ArrayList<>(base.issues());
        if (request.message().isBlank()) {
            issues.add(ValidationIssue.error(null, "message", "validation.commit.message.required"));
        }
        if (request.rollbackOf().isPresent() && request.rollbackOf().getAsLong() > request.baseVersion()) {
            issues.add(ValidationIssue.error(null, "rollbackOf", "validation.rollback.version.invalid",
                    Long.toString(request.rollbackOf().getAsLong())));
        }
        return new ValidationReport(issues);
    }

    private List<FileConflict> detectConflicts(WorkspaceHead head, RenderPlan newPlan,
                                               SortedMap<ArtifactPath, FileObservation> confirmations)
            throws IOException {
        Set<ArtifactPath> paths = new HashSet<>(head.artifacts().artifacts().keySet());
        paths.addAll(newPlan.artifacts().keySet());
        paths.addAll(confirmations.keySet());
        List<FileConflict> conflicts = new ArrayList<>();
        for (ArtifactPath path : paths.stream().sorted().toList()) {
            FileObservation expected = head.artifacts().artifacts().containsKey(path)
                    ? FileObservation.existing(head.artifacts().artifacts().get(path).sha256())
                    : FileObservation.missing();
            Path actualPath = this.resolver.resolveArtifact(path);
            FileObservation actual = observe(actualPath);
            ConflictReason reason = conflictReason(expected, actual);
            FileObservation confirmation = confirmations.get(path);
            if (confirmation != null && !confirmation.equals(actual)) {
                conflicts.add(new FileConflict(path, actualPath,
                        ConflictReason.TAKEOVER_CONFIRMATION_STALE, confirmation, actual));
            } else if (reason != null && confirmation == null) {
                conflicts.add(new FileConflict(path, actualPath, reason, expected, actual));
            }
        }
        return List.copyOf(conflicts);
    }

    private List<TransactionMutation> mutations(Revision revision, WorkspaceHead head,
                                                SortedMap<ArtifactPath, FileObservation> confirmations)
            throws IOException {
        List<TransactionMutation> mutations = new ArrayList<>();
        if (!confirmations.isEmpty()) {
            String backupRoot = "external-backups/" + BACKUP_TIME.format(revision.committedAt())
                    + "-v" + revision.version() + "/";
            for (Map.Entry<ArtifactPath, FileObservation> confirmation : confirmations.entrySet()) {
                if (!confirmation.getValue().exists()) {
                    continue;
                }
                Path source = this.resolver.resolveArtifact(confirmation.getKey());
                byte[] content = Files.readAllBytes(source);
                String actualHash = ContentHashes.sha256(content);
                if (!actualHash.equals(confirmation.getValue().sha256().orElseThrow())) {
                    throw new IOException("External file changed while preparing takeover: " + source);
                }
                mutations.add(TransactionMutation.write(TransactionScope.STATE,
                        ArtifactPath.of(backupRoot + confirmation.getKey().value()), content));
            }
        }

        revision.artifacts().artifacts().values().forEach(artifact -> mutations.add(
                TransactionMutation.write(TransactionScope.GAME, artifact.logicalPath(), artifact.utf8Content())));
        revision.deletedPaths().forEach(path -> mutations.add(
                TransactionMutation.delete(TransactionScope.GAME, path)));

        byte[] revisionBytes = this.repository.encode(revision).getBytes(StandardCharsets.UTF_8);
        mutations.add(TransactionMutation.write(TransactionScope.STATE,
                ArtifactPath.of("history/" + String.format("%010d.json", revision.version())), revisionBytes));
        mutations.add(TransactionMutation.write(TransactionScope.STATE,
                ArtifactPath.of("workspace.json"), revisionBytes));
        byte[] draftBytes = this.draftCodec.encode(DraftState.committed(revision.version(),
                revision.snapshot(), revision.committedAt())).getBytes(StandardCharsets.UTF_8);
        mutations.add(TransactionMutation.write(TransactionScope.STATE,
                ArtifactPath.of("draft.json"), draftBytes));
        return List.copyOf(mutations);
    }

    private static ConflictReason conflictReason(FileObservation expected, FileObservation actual) {
        if (!expected.exists() && actual.exists()) {
            return ConflictReason.UNMANAGED_PATH_OCCUPIED;
        }
        if (expected.exists() && !actual.exists()) {
            return ConflictReason.MANAGED_FILE_DELETED;
        }
        if (expected.exists() && !expected.sha256().equals(actual.sha256())) {
            return ConflictReason.MANAGED_FILE_MODIFIED;
        }
        return null;
    }

    private static FileObservation observe(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return FileObservation.missing();
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return FileObservation.existing(ContentHashes.sha256("non-regular:" + path.getFileName()));
        }
        return FileObservation.existing(ContentHashes.sha256(Files.readAllBytes(path)));
    }

    private static TreeMap<ArtifactPath, String> hashes(RenderPlan plan) {
        TreeMap<ArtifactPath, String> hashes = new TreeMap<>();
        plan.artifacts().forEach((path, artifact) -> hashes.put(path, artifact.sha256()));
        return hashes;
    }

    private static FileLock tryLock(FileChannel channel) throws IOException {
        try {
            return channel.tryLock();
        } catch (OverlappingFileLockException exception) {
            return null;
        }
    }

    private static CommitResult.IoFailure ioFailure(String code, ArtifactPath path, Throwable throwable) {
        String message = throwable.getMessage() == null
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
        return new CommitResult.IoFailure(code, Optional.ofNullable(path), message);
    }

}
