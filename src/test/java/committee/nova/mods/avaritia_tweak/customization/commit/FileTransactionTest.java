package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTransactionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void recoversPreparedTransactionLeftBySimulatedProcessCrash() throws Exception {
        Path gameRoot = this.temporaryDirectory.resolve("game");
        Path stateRoot = this.temporaryDirectory.resolve("state");
        Path transactions = stateRoot.resolve("transactions");
        Path existing = gameRoot.resolve("managed/existing.txt");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "before", StandardCharsets.UTF_8);
        ManagedPathResolver resolver = new ManagedPathResolver(gameRoot, stateRoot);
        FileTransaction crashing = new FileTransaction(resolver, transactions, (index, target) -> {
            if (index == 1) {
                throw new SimulatedCrash();
            }
        });
        List<TransactionMutation> mutations = List.of(
                TransactionMutation.write(TransactionScope.GAME,
                        ArtifactPath.of("managed/existing.txt"), "after".getBytes(StandardCharsets.UTF_8)),
                TransactionMutation.write(TransactionScope.GAME,
                        ArtifactPath.of("managed/new.txt"), "new".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> crashing.apply(mutations)).isInstanceOf(SimulatedCrash.class);
        assertThat(Files.readString(existing)).isEqualTo("after");

        new FileTransaction(resolver, transactions).recoverPending();

        assertThat(Files.readString(existing)).isEqualTo("before");
        assertThat(Files.exists(gameRoot.resolve("managed/new.txt"))).isFalse();
        try (var pending = Files.list(transactions)) {
            assertThat(pending.toList()).isEmpty();
        }
    }

    private static final class SimulatedCrash extends Error {
    }
}
