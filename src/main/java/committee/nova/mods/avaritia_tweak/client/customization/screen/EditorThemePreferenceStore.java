package committee.nova.mods.avaritia_tweak.client.customization.screen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

/** Persists the selected visual theme without coupling it to workspace history or server state. */
final class EditorThemePreferenceStore {
    private final Path file;

    EditorThemePreferenceStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    LoadResult load() {
        if (!Files.isRegularFile(this.file)) {
            return new LoadResult(EditorThemeStyle.CLASSIC, Optional.empty());
        }
        try {
            String id = Files.readString(this.file, StandardCharsets.UTF_8).strip();
            Optional<EditorThemeStyle> style = EditorThemeStyle.find(id);
            if (style.isPresent()) {
                return new LoadResult(style.get(), Optional.empty());
            }
            return new LoadResult(EditorThemeStyle.CLASSIC,
                    Optional.of("Unknown editor theme '" + id + "' in " + this.file));
        } catch (IOException exception) {
            return new LoadResult(EditorThemeStyle.CLASSIC,
                    Optional.of("Unable to read " + this.file + ": " + safeMessage(exception)));
        }
    }

    Optional<String> save(EditorThemeStyle style) {
        Objects.requireNonNull(style, "style");
        Path temporary = this.file.resolveSibling(this.file.getFileName() + ".tmp");
        try {
            Files.createDirectories(this.file.getParent());
            Files.writeString(temporary, style.id() + "\n", StandardCharsets.UTF_8);
            moveReplace(temporary, this.file);
            return Optional.empty();
        } catch (IOException exception) {
            return Optional.of("Unable to save " + this.file + ": " + safeMessage(exception));
        }
    }

    Path file() {
        return this.file;
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

    record LoadResult(EditorThemeStyle style, Optional<String> warning) {
        LoadResult {
            Objects.requireNonNull(style, "style");
            warning = Objects.requireNonNull(warning, "warning");
        }
    }
}
