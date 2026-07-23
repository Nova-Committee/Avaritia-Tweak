package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;

import java.nio.file.Path;
import java.util.Objects;

public final class ManagedPathResolver {
    private final Path gameRoot;
    private final Path stateRoot;

    public ManagedPathResolver(Path gameRoot, Path stateRoot) {
        this.gameRoot = Objects.requireNonNull(gameRoot, "gameRoot").toAbsolutePath().normalize();
        this.stateRoot = Objects.requireNonNull(stateRoot, "stateRoot").toAbsolutePath().normalize();
    }

    public Path resolveArtifact(ArtifactPath logicalPath) {
        return logicalPath.resolveWithin(this.gameRoot);
    }

    public Path resolveState(ArtifactPath relativePath) {
        return relativePath.resolveWithin(this.stateRoot);
    }

    public Path gameRoot() {
        return this.gameRoot;
    }

    public Path stateRoot() {
        return this.stateRoot;
    }
}
