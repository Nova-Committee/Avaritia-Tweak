package committee.nova.mods.avaritia_tweak.customization.history;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class LocalWorkspaceRepository {
    private static final Pattern HISTORY_FILE = Pattern.compile("\\d{10}\\.json");
    private final Path stateRoot;
    private final RevisionCodec revisionCodec;

    public LocalWorkspaceRepository(Path stateRoot, RevisionCodec revisionCodec) {
        this.stateRoot = Objects.requireNonNull(stateRoot, "stateRoot").toAbsolutePath().normalize();
        this.revisionCodec = Objects.requireNonNull(revisionCodec, "revisionCodec");
    }

    public WorkspaceHead loadHead() throws RepositoryException {
        if (!Files.exists(workspaceFile())) {
            return WorkspaceHead.initial();
        }
        Revision revision = readRevision(workspaceFile());
        return new WorkspaceHead(revision.version(), revision.snapshot(), revision.artifacts());
    }

    public Revision loadRevision(long version) throws RepositoryException {
        if (version <= 0) {
            throw new RepositoryException("history.version.invalid", historyFile(version),
                    "History versions start at 1");
        }
        Path path = historyFile(version);
        if (!Files.isRegularFile(path)) {
            throw new RepositoryException("history.version.missing", path,
                    "History revision " + version + " does not exist");
        }
        Revision revision = readRevision(path);
        if (revision.version() != version) {
            throw new RepositoryException("history.version.mismatch", path,
                    "History filename and revision version do not match");
        }
        return revision;
    }

    public List<Revision> loadHistory() throws RepositoryException {
        if (!Files.isDirectory(historyDirectory())) {
            return List.of();
        }
        try (var paths = Files.list(historyDirectory())) {
            List<Path> revisionPaths = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> HISTORY_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            List<Revision> revisions = new ArrayList<>();
            for (Path path : revisionPaths) {
                revisions.add(readRevision(path));
            }
            return List.copyOf(revisions);
        } catch (IOException exception) {
            throw new RepositoryException("history.list.failed", historyDirectory(),
                    "Unable to list history", exception);
        }
    }

    public String encode(Revision revision) {
        return this.revisionCodec.encode(revision);
    }

    public Path stateRoot() {
        return this.stateRoot;
    }

    public Path workspaceFile() {
        return this.stateRoot.resolve("workspace.json");
    }

    public Path draftFile() {
        return this.stateRoot.resolve("draft.json");
    }

    public Path lockFile() {
        return this.stateRoot.resolve("commit.lock");
    }

    public Path historyDirectory() {
        return this.stateRoot.resolve("history");
    }

    public Path historyFile(long version) {
        return historyDirectory().resolve(String.format("%010d.json", version));
    }

    public Path transactionsDirectory() {
        return this.stateRoot.resolve("transactions");
    }

    public Path externalBackupsDirectory() {
        return this.stateRoot.resolve("external-backups");
    }

    private Revision readRevision(Path path) throws RepositoryException {
        final String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RepositoryException("history.read.failed", path,
                    "Unable to read revision", exception);
        }
        RevisionDecodeResult decoded = this.revisionCodec.decode(content);
        if (decoded instanceof RevisionDecodeResult.Failure failure) {
            throw new RepositoryException(failure.code(), path,
                    failure.fieldPath() + ": " + failure.message());
        }
        return ((RevisionDecodeResult.Success) decoded).revision();
    }
}
