package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.customization.history.LocalWorkspaceRepository;
import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;

import java.util.List;
import java.util.Objects;

public final class LocalCustomizationHistory implements CustomizationHistory {
    private final LocalWorkspaceRepository repository;

    public LocalCustomizationHistory(LocalWorkspaceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public WorkspaceHead loadHead() throws RepositoryException {
        return this.repository.loadHead();
    }

    @Override
    public List<Revision> loadHistory() throws RepositoryException {
        return this.repository.loadHistory();
    }

    @Override
    public Revision loadRevision(long version) throws RepositoryException {
        return this.repository.loadRevision(version);
    }
}
