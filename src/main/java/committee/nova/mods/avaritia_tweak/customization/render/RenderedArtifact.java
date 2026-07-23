package committee.nova.mods.avaritia_tweak.customization.render;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public record RenderedArtifact(ArtifactPath logicalPath, byte[] utf8Content, String sha256) {
    public RenderedArtifact {
        Objects.requireNonNull(logicalPath, "logicalPath");
        utf8Content = Objects.requireNonNull(utf8Content, "utf8Content").clone();
        Objects.requireNonNull(sha256, "sha256");
        String actualHash = ContentHashes.sha256(utf8Content);
        if (!actualHash.equals(sha256)) {
            throw new IllegalArgumentException("Artifact checksum does not match content: " + logicalPath);
        }
    }

    public static RenderedArtifact text(ArtifactPath path, String content) {
        String normalized = normalizeText(content);
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        return new RenderedArtifact(path, bytes, ContentHashes.sha256(bytes));
    }

    @Override
    public byte[] utf8Content() {
        return this.utf8Content.clone();
    }

    public String text() {
        return new String(this.utf8Content, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RenderedArtifact artifact
                && this.logicalPath.equals(artifact.logicalPath)
                && Arrays.equals(this.utf8Content, artifact.utf8Content)
                && this.sha256.equals(artifact.sha256);
    }

    @Override
    public int hashCode() {
        int result = this.logicalPath.hashCode();
        result = 31 * result + Arrays.hashCode(this.utf8Content);
        result = 31 * result + this.sha256.hashCode();
        return result;
    }

    private static String normalizeText(String content) {
        String normalized = Objects.requireNonNull(content, "content")
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        int end = normalized.length();
        while (end > 0 && normalized.charAt(end - 1) == '\n') {
            end--;
        }
        return normalized.substring(0, end) + "\n";
    }
}
