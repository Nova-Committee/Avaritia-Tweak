package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;
import committee.nova.mods.avaritia_tweak.customization.history.LocalWorkspaceRepository;
import committee.nova.mods.avaritia_tweak.customization.history.RevisionCodec;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientStartupStateTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exposesCorruptWorkspaceAsReadOnlyStartupFailure() throws Exception {
        LocalWorkspaceRepository repository = repository();
        Files.createDirectories(this.temporaryDirectory);
        Files.writeString(repository.workspaceFile(), "{not valid revision json");

        ClientStartupState startup = ClientStartupState.load(repository, Optional.empty());

        assertEquals(0, startup.head().version());
        assertTrue(startup.head().snapshot().entries().isEmpty());
        CommitResult.IoFailure failure = assertInstanceOf(CommitResult.IoFailure.class,
                startup.failure().orElseThrow());
        assertEquals("revision.json.invalid", failure.code());
        assertTrue(failure.message().contains("workspace.json"));
    }

    @Test
    void keepsMoreUrgentRecoveryFailureWhenHeadIsAlsoUnreadable() throws Exception {
        LocalWorkspaceRepository repository = repository();
        Files.createDirectories(this.temporaryDirectory);
        Files.writeString(repository.workspaceFile(), "broken");
        CommitResult recovery = new CommitResult.RecoveryFailure("pending transaction");

        ClientStartupState startup = ClientStartupState.load(repository, Optional.of(recovery));

        assertEquals(Optional.of(recovery), startup.failure());
        assertEquals(0, startup.head().version());
    }

    private LocalWorkspaceRepository repository() {
        return new LocalWorkspaceRepository(this.temporaryDirectory,
                new RevisionCodec(new WorkspaceCodec()));
    }
}
