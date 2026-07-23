package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;

import java.util.List;

public interface CustomizationHistory {
    WorkspaceHead loadHead() throws RepositoryException;

    List<Revision> loadHistory() throws RepositoryException;

    Revision loadRevision(long version) throws RepositoryException;
}
