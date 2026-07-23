package committee.nova.mods.avaritia_tweak.customization.commit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FileTransaction {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final ManagedPathResolver resolver;
    private final Path transactionsDirectory;
    private final TransactionFaultInjector faultInjector;

    public FileTransaction(ManagedPathResolver resolver, Path transactionsDirectory) {
        this(resolver, transactionsDirectory, TransactionFaultInjector.NONE);
    }

    public FileTransaction(ManagedPathResolver resolver, Path transactionsDirectory,
                           TransactionFaultInjector faultInjector) {
        this.resolver = resolver;
        this.transactionsDirectory = transactionsDirectory.toAbsolutePath().normalize();
        this.faultInjector = faultInjector;
    }

    public void recoverPending() throws FileTransactionException {
        if (!Files.isDirectory(this.transactionsDirectory)) {
            return;
        }
        final List<Path> directories;
        try (var paths = Files.list(this.transactionsDirectory)) {
            directories = paths.filter(Files::isDirectory).sorted().toList();
        } catch (IOException exception) {
            throw new FileTransactionException("Unable to list pending transactions", true, exception);
        }
        for (Path directory : directories) {
            Path journal = directory.resolve("journal.json");
            if (!Files.isRegularFile(journal)) {
                try {
                    deleteTree(directory);
                } catch (IOException exception) {
                    throw new FileTransactionException("Unable to clean an incomplete transaction", true,
                            exception);
                }
                continue;
            }
            final Journal decoded;
            try {
                decoded = readJournal(journal);
            } catch (IOException | RuntimeException exception) {
                throw new FileTransactionException("Unable to read transaction journal " + journal,
                        true, exception);
            }
            try {
                if (decoded.state.equals("PREPARED")) {
                    rollback(directory, decoded.entries);
                } else if (!decoded.state.equals("COMMITTED")) {
                    throw new IOException("Unknown transaction state " + decoded.state);
                }
                deleteTree(directory);
            } catch (IOException exception) {
                throw new FileTransactionException("Unable to recover transaction " + directory.getFileName(),
                        true, exception);
            }
        }
    }

    public void apply(List<TransactionMutation> requestedMutations) throws FileTransactionException {
        List<TransactionMutation> mutations = List.copyOf(requestedMutations);
        validateUnique(mutations);
        Path directory = this.transactionsDirectory.resolve(UUID.randomUUID().toString());
        Path journalPath = directory.resolve("journal.json");
        List<JournalEntry> entries = new ArrayList<>();
        boolean prepared = false;
        boolean committed = false;
        try {
            Files.createDirectories(directory.resolve("staged"));
            Files.createDirectories(directory.resolve("before"));
            for (TransactionMutation mutation : mutations) {
                Path target = resolve(mutation.scope(), mutation.path());
                verifyNoSymbolicLinks(mutation.scope(), target);
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(target,
                        LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Managed target is not a regular file: " + target);
                }
                boolean existed = Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS);
                String transactionPath = transactionPath(mutation.scope(), mutation.path());
                if (existed) {
                    Path before = directory.resolve("before").resolve(transactionPath);
                    Files.createDirectories(before.getParent());
                    copyForced(target, before);
                }
                if (mutation.content().isPresent()) {
                    Path staged = directory.resolve("staged").resolve(transactionPath);
                    Files.createDirectories(staged.getParent());
                    writeForced(staged, mutation.content().orElseThrow());
                }
                entries.add(new JournalEntry(mutation.scope(), mutation.path(), existed,
                        mutation.content().isPresent()));
            }
            writeJournal(journalPath, new Journal("PREPARED", entries));
            prepared = true;
            for (int index = 0; index < entries.size(); index++) {
                JournalEntry entry = entries.get(index);
                Path target = resolve(entry.scope, entry.path);
                this.faultInjector.beforeApply(index, target);
                if (entry.write) {
                    Path staged = directory.resolve("staged")
                            .resolve(transactionPath(entry.scope, entry.path));
                    Files.createDirectories(target.getParent());
                    moveReplace(staged, target);
                } else {
                    Files.deleteIfExists(target);
                }
            }
            writeJournal(journalPath, new Journal("COMMITTED", entries));
            committed = true;
            try {
                deleteTree(directory);
            } catch (IOException ignored) {
                // The durable COMMITTED journal makes deferred cleanup safe on the next startup.
            }
        } catch (IOException | RuntimeException exception) {
            if (committed) {
                return;
            } else if (prepared) {
                try {
                    rollback(directory, entries);
                    deleteTree(directory);
                } catch (IOException recoveryException) {
                    recoveryException.addSuppressed(exception);
                    throw new FileTransactionException("Transaction failed and rollback was incomplete",
                            true, recoveryException);
                }
            } else {
                try {
                    deleteTree(directory);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw new FileTransactionException("Transaction failed: " + safeMessage(exception), false, exception);
        }
    }

    private void rollback(Path directory, List<JournalEntry> entries) throws IOException {
        IOException failure = null;
        for (JournalEntry entry : entries) {
            Path target = resolve(entry.scope, entry.path);
            try {
                if (entry.existed) {
                    Path before = directory.resolve("before")
                            .resolve(transactionPath(entry.scope, entry.path));
                    if (!Files.isRegularFile(before, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Missing transaction before-image: " + before);
                    }
                    Path restore = target.resolveSibling(target.getFileName() + ".avaritia-tweak-restore");
                    Files.createDirectories(target.getParent());
                    copyForced(before, restore);
                    moveReplace(restore, target);
                } else {
                    Files.deleteIfExists(target);
                }
            } catch (IOException exception) {
                if (failure == null) {
                    failure = new IOException("Unable to restore every transaction target");
                }
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private Path resolve(TransactionScope scope, ArtifactPath path) {
        return scope == TransactionScope.GAME
                ? this.resolver.resolveArtifact(path)
                : this.resolver.resolveState(path);
    }

    private void verifyNoSymbolicLinks(TransactionScope scope, Path target) throws IOException {
        Path root = scope == TransactionScope.GAME ? this.resolver.gameRoot() : this.resolver.stateRoot();
        Path current = root;
        Path relative = root.relativize(target);
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IOException("Symbolic links are not allowed in managed paths: " + current);
            }
        }
    }

    private static void validateUnique(List<TransactionMutation> mutations) {
        Set<String> identities = new HashSet<>();
        for (TransactionMutation mutation : mutations) {
            String identity = mutation.scope() + ":" + mutation.path().value();
            if (!identities.add(identity)) {
                throw new IllegalArgumentException("Duplicate transaction target: " + identity);
            }
        }
    }

    private static String transactionPath(TransactionScope scope, ArtifactPath path) {
        return scope.name().toLowerCase(java.util.Locale.ROOT) + "/" + path.value();
    }

    private static void writeForced(Path path, byte[] content) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(content);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private static void copyForced(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeJournal(Path path, Journal journal) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("state", journal.state);
        JsonArray entries = new JsonArray();
        for (JournalEntry entry : journal.entries) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("scope", entry.scope.name());
            encoded.addProperty("path", entry.path.value());
            encoded.addProperty("existed", entry.existed);
            encoded.addProperty("write", entry.write);
            entries.add(encoded);
        }
        root.add("entries", entries);
        writeForced(path, (GSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static Journal readJournal(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        List<JournalEntry> entries = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject encoded = element.getAsJsonObject();
            entries.add(new JournalEntry(TransactionScope.valueOf(encoded.get("scope").getAsString()),
                    ArtifactPath.of(encoded.get("path").getAsString()),
                    encoded.get("existed").getAsBoolean(), encoded.get("write").getAsBoolean()));
        }
        return new Journal(root.get("state").getAsString(), entries);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : ordered) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private record Journal(String state, List<JournalEntry> entries) {
    }

    private record JournalEntry(TransactionScope scope, ArtifactPath path, boolean existed, boolean write) {
    }
}
