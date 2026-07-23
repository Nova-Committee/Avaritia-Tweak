package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;

import java.util.Objects;
import java.util.Optional;

public record TransactionMutation(TransactionScope scope, ArtifactPath path, Optional<byte[]> content) {
    public TransactionMutation {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(path, "path");
        content = Objects.requireNonNull(content, "content").map(byte[]::clone);
    }

    public static TransactionMutation write(TransactionScope scope, ArtifactPath path, byte[] content) {
        return new TransactionMutation(scope, path, Optional.of(content));
    }

    public static TransactionMutation delete(TransactionScope scope, ArtifactPath path) {
        return new TransactionMutation(scope, path, Optional.empty());
    }

    @Override
    public Optional<byte[]> content() {
        return this.content.map(byte[]::clone);
    }
}
