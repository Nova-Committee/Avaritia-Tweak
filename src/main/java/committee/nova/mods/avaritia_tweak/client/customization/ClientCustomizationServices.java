package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftCodec;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftLoadResult;
import committee.nova.mods.avaritia_tweak.client.customization.persistence.DraftStore;
import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;
import committee.nova.mods.avaritia_tweak.customization.commit.LocalFileCommitTarget;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceCodec;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceHead;
import committee.nova.mods.avaritia_tweak.customization.render.PreviewService;
import committee.nova.mods.avaritia_tweak.customization.render.WorkspaceRenderService;
import committee.nova.mods.avaritia_tweak.customization.validation.MinecraftRegistryLookup;
import committee.nova.mods.avaritia_tweak.customization.validation.WorkspaceValidator;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;

public final class ClientCustomizationServices {
    private static ClientCustomizationServices instance;
    private final Path gameRoot;
    private final Path stateRoot;
    private final EditorController controller;
    private final Optional<CommitResult> startupFailure;

    private ClientCustomizationServices() {
        this.gameRoot = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        this.stateRoot = FMLPaths.CONFIGDIR.get().resolve("avaritia_tweak/visual_editor")
                .toAbsolutePath().normalize();
        WorkspaceCodec workspaceCodec = new WorkspaceCodec();
        WorkspaceValidator validator = new WorkspaceValidator(new MinecraftRegistryLookup());
        WorkspaceRenderService renderer = WorkspaceRenderService.standard();
        LocalFileCommitTarget target = new LocalFileCommitTarget(this.gameRoot, this.stateRoot,
                validator, renderer);
        ClientStartupState startup = ClientStartupState.load(target.repository(),
                target.recoverPendingTransactions());
        WorkspaceHead head = startup.head();
        this.startupFailure = startup.failure();
        DraftStore draftStore = new DraftStore(target.repository().draftFile(),
                new DraftCodec(workspaceCodec));
        DraftLoadResult draft = draftStore.load(head);
        this.controller = new EditorController(head, draft, new PreviewService(validator, renderer),
                target, new LocalCustomizationHistory(target.repository()), draftStore, Clock.systemUTC());
    }

    public static synchronized ClientCustomizationServices get() {
        if (instance == null) {
            instance = new ClientCustomizationServices();
        }
        return instance;
    }

    public EditorController controller() {
        return this.controller;
    }

    public Path gameRoot() {
        return this.gameRoot;
    }

    public Path stateRoot() {
        return this.stateRoot;
    }

    public Optional<CommitResult> startupFailure() {
        return this.startupFailure;
    }
}
