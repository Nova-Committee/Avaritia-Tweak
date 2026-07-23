package committee.nova.mods.avaritia_tweak.client.customization.persistence;

import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class DraftStore implements AutoCloseable {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private final Path draftFile;
    private final DraftCodec codec;
    private final Clock clock;
    private final ScheduledExecutorService executor;
    private final long debounceMillis;
    private final Object monitor = new Object();
    private DraftState pending;
    private ScheduledFuture<?> scheduled;
    private DraftSaveResult.Failure lastFailure;

    public DraftStore(Path draftFile, DraftCodec codec) {
        this(draftFile, codec, Clock.systemUTC(), 500);
    }

    DraftStore(Path draftFile, DraftCodec codec, Clock clock, long debounceMillis) {
        this.draftFile = Objects.requireNonNull(draftFile, "draftFile").toAbsolutePath().normalize();
        this.codec = Objects.requireNonNull(codec, "codec");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.debounceMillis = debounceMillis;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "avaritia-tweak-draft-writer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public DraftLoadResult load(WorkspaceHead fallbackHead) {
        DraftState fallback = DraftState.committed(fallbackHead.version(), fallbackHead.snapshot(), this.clock.instant());
        if (!Files.isRegularFile(this.draftFile)) {
            return new DraftLoadResult.Missing(fallback);
        }
        try {
            DraftDecodeResult decoded = this.codec.decode(Files.readString(this.draftFile, StandardCharsets.UTF_8));
            if (decoded instanceof DraftDecodeResult.Success success) {
                return new DraftLoadResult.Loaded(success.draft());
            }
            DraftDecodeResult.Failure failure = (DraftDecodeResult.Failure) decoded;
            Path preserved = preserveCorruptCopy();
            return new DraftLoadResult.Corrupt(fallback, preserved, failure);
        } catch (IOException exception) {
            DraftDecodeResult.Failure failure = new DraftDecodeResult.Failure(
                    "draft.read.failed", "$", safeMessage(exception));
            try {
                return new DraftLoadResult.Corrupt(fallback, preserveCorruptCopy(), failure);
            } catch (IOException preserveFailure) {
                return new DraftLoadResult.Corrupt(fallback, this.draftFile, new DraftDecodeResult.Failure(
                        "draft.preserve.failed", "$", safeMessage(preserveFailure)));
            }
        }
    }

    public void schedule(DraftState draft) {
        synchronized (this.monitor) {
            this.pending = Objects.requireNonNull(draft, "draft");
            if (this.scheduled != null) {
                this.scheduled.cancel(false);
            }
            this.scheduled = this.executor.schedule(this::flushScheduled,
                    this.debounceMillis, TimeUnit.MILLISECONDS);
        }
    }

    public DraftSaveResult flush() {
        DraftState toWrite;
        synchronized (this.monitor) {
            if (this.scheduled != null) {
                this.scheduled.cancel(false);
                this.scheduled = null;
            }
            toWrite = this.pending;
            this.pending = null;
        }
        if (toWrite == null) {
            return this.lastFailure == null
                    ? new DraftSaveResult.Success(this.draftFile)
                    : this.lastFailure;
        }
        DraftSaveResult result = write(toWrite);
        synchronized (this.monitor) {
            this.lastFailure = result instanceof DraftSaveResult.Failure failure ? failure : null;
        }
        return result;
    }

    public Optional<DraftSaveResult.Failure> lastFailure() {
        synchronized (this.monitor) {
            return Optional.ofNullable(this.lastFailure);
        }
    }

    @Override
    public void close() {
        flush();
        this.executor.shutdown();
    }

    private void flushScheduled() {
        flush();
    }

    private DraftSaveResult write(DraftState draft) {
        Path temporary = this.draftFile.resolveSibling(this.draftFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(this.draftFile.getParent());
            byte[] content = this.codec.encode(draft).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(content));
                channel.force(true);
            }
            moveReplace(temporary, this.draftFile);
            return new DraftSaveResult.Success(this.draftFile);
        } catch (IOException exception) {
            return new DraftSaveResult.Failure(this.draftFile, safeMessage(exception));
        }
    }

    private Path preserveCorruptCopy() throws IOException {
        String timestamp = BACKUP_TIME.format(this.clock.instant());
        Path candidate = this.draftFile.resolveSibling(this.draftFile.getFileName() + ".corrupt-" + timestamp);
        int suffix = 0;
        while (Files.exists(candidate)) {
            suffix++;
            candidate = this.draftFile.resolveSibling(this.draftFile.getFileName()
                    + ".corrupt-" + timestamp + "-" + suffix);
        }
        Files.copy(this.draftFile, candidate);
        return candidate;
    }

    private static void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
