package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;
import committee.nova.mods.avaritia_tweak.customization.history.LocalWorkspaceRepository;
import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;

import java.util.Objects;
import java.util.Optional;

record ClientStartupState(WorkspaceHead head, Optional<CommitResult> failure) {
    ClientStartupState {
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(failure, "failure");
    }

    static ClientStartupState load(LocalWorkspaceRepository repository,
                                   Optional<CommitResult> recoveryFailure) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(recoveryFailure, "recoveryFailure");
        try {
            return new ClientStartupState(repository.loadHead(), recoveryFailure);
        } catch (RepositoryException exception) {
            CommitResult failure = recoveryFailure.orElseGet(() -> new CommitResult.IoFailure(
                    exception.code(), Optional.empty(), exception.path() + ": " + exception.getMessage()));
            return new ClientStartupState(WorkspaceHead.initial(), Optional.of(failure));
        }
    }
}
