package committee.nova.mods.avaritia_tweak.customization.render;

import java.nio.file.Path;
import java.util.Objects;

public record ArtifactPath(String value) implements Comparable<ArtifactPath> {
    public ArtifactPath {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.startsWith("/") || value.endsWith("/")
                || value.indexOf('\\') >= 0 || value.indexOf('\0') >= 0 || value.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Unsafe artifact path: " + value);
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Unsafe artifact path segment: " + value);
            }
        }
    }

    public static ArtifactPath of(String value) {
        return new ArtifactPath(value);
    }

    public Path resolveWithin(Path root) {
        Path normalizedRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(this.value).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Artifact path escapes its root: " + this.value);
        }
        return resolved;
    }

    @Override
    public int compareTo(ArtifactPath other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
