package committee.nova.mods.avaritia_tweak.customization.history;

import java.nio.file.Path;

public final class RepositoryException extends Exception {
    private final String code;
    private final Path path;

    public RepositoryException(String code, Path path, String message) {
        super(message);
        this.code = code;
        this.path = path;
    }

    public RepositoryException(String code, Path path, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.path = path;
    }

    public String code() {
        return this.code;
    }

    public Path path() {
        return this.path;
    }
}
