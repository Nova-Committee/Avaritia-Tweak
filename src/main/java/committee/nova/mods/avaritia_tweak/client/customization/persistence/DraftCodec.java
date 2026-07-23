package committee.nova.mods.avaritia_tweak.client.customization.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceCodec;
import committee.nova.mods.avaritia_tweak.customization.history.WorkspaceDecodeResult;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.util.Optional;

public final class DraftCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final WorkspaceCodec workspaceCodec;

    public DraftCodec(WorkspaceCodec workspaceCodec) {
        this.workspaceCodec = workspaceCodec;
    }

    public String encode(DraftState draft) {
        JsonObject root = new JsonObject();
        root.addProperty("draftSchemaVersion", draft.schemaVersion());
        root.addProperty("baseVersion", draft.baseVersion());
        root.addProperty("savedAt", draft.savedAt().toString());
        root.add("workspace", this.workspaceCodec.toJson(draft.workspace()));
        draft.selectedEntry().ifPresent(key -> {
            JsonObject selected = new JsonObject();
            selected.addProperty("kind", key.kind().name());
            selected.addProperty("id", key.id().toString());
            root.add("selectedEntry", selected);
        });
        root.addProperty("activePanel", draft.activePanel());
        return GSON.toJson(root) + "\n";
    }

    public DraftDecodeResult decode(String content) {
        try {
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                return failure("draft.root", "$", "Draft root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            int schema = root.get("draftSchemaVersion").getAsInt();
            if (schema != DraftState.CURRENT_SCHEMA_VERSION) {
                return failure("draft.schema.unsupported", "draftSchemaVersion",
                        "Unsupported draft schema " + schema);
            }
            WorkspaceDecodeResult decodedWorkspace = this.workspaceCodec.decode(root.getAsJsonObject("workspace"));
            if (decodedWorkspace instanceof WorkspaceDecodeResult.Failure failure) {
                return failure("draft.workspace." + failure.code(), "workspace." + failure.fieldPath(),
                        failure.message());
            }
            Optional<EntryKey> selected = Optional.empty();
            if (root.has("selectedEntry")) {
                JsonObject selectedJson = root.getAsJsonObject("selectedEntry");
                EntryKind kind = EntryKind.valueOf(selectedJson.get("kind").getAsString());
                ResourceLocation id = ResourceLocation.tryParse(selectedJson.get("id").getAsString());
                if (id == null) {
                    return failure("draft.selected.id", "selectedEntry.id", "Invalid selected entry ID");
                }
                selected = Optional.of(new EntryKey(kind, id));
            }
            return new DraftDecodeResult.Success(new DraftState(schema,
                    root.get("baseVersion").getAsLong(), Instant.parse(root.get("savedAt").getAsString()),
                    ((WorkspaceDecodeResult.Success) decodedWorkspace).snapshot(), selected,
                    root.get("activePanel").getAsString()));
        } catch (RuntimeException exception) {
            return failure("draft.json.invalid", "$",
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private static DraftDecodeResult.Failure failure(String code, String path, String message) {
        return new DraftDecodeResult.Failure(code, path, message);
    }
}
