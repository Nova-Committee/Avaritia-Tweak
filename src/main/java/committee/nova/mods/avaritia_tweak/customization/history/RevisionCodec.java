package committee.nova.mods.avaritia_tweak.customization.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.ContentHashes;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;
import committee.nova.mods.avaritia_tweak.customization.render.RenderedArtifact;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public final class RevisionCodec {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private final WorkspaceCodec workspaceCodec;

    public RevisionCodec(WorkspaceCodec workspaceCodec) {
        this.workspaceCodec = workspaceCodec;
    }

    public String encode(Revision revision) {
        JsonObject root = new JsonObject();
        root.addProperty("revisionSchemaVersion", CURRENT_SCHEMA_VERSION);
        root.addProperty("version", revision.version());
        root.addProperty("parentVersion", revision.parentVersion());
        root.addProperty("committedAt", revision.committedAt().toString());
        root.addProperty("message", revision.message());
        JsonObject summary = new JsonObject();
        summary.addProperty("added", revision.summary().added());
        summary.addProperty("modified", revision.summary().modified());
        summary.addProperty("removed", revision.summary().removed());
        root.add("summary", summary);
        revision.rollbackOf().ifPresent(value -> root.addProperty("rollbackOf", value));
        root.addProperty("snapshotSha256", revision.snapshotSha256());
        root.add("snapshot", this.workspaceCodec.toJson(revision.snapshot()));

        JsonArray artifacts = new JsonArray();
        revision.artifacts().artifacts().values().forEach(artifact -> {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("path", artifact.logicalPath().value());
            encoded.addProperty("sha256", artifact.sha256());
            encoded.addProperty("content", artifact.text());
            artifacts.add(encoded);
        });
        root.add("artifacts", artifacts);
        JsonArray deletedPaths = new JsonArray();
        revision.deletedPaths().forEach(path -> deletedPaths.add(path.value()));
        root.add("deletedPaths", deletedPaths);
        return GSON.toJson(root) + "\n";
    }

    public RevisionDecodeResult decode(String content) {
        try {
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                return failure("revision.root", "$", "Revision root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            int schemaVersion = integer(root, "revisionSchemaVersion");
            if (schemaVersion != CURRENT_SCHEMA_VERSION) {
                return failure("revision.schema.unsupported", "revisionSchemaVersion",
                        "Unsupported revision schema " + schemaVersion);
            }
            long version = longValue(root, "version");
            long parentVersion = longValue(root, "parentVersion");
            Instant committedAt;
            try {
                committedAt = Instant.parse(string(root, "committedAt"));
            } catch (DateTimeParseException exception) {
                return failure("revision.time.invalid", "committedAt", exception.getMessage());
            }
            String message = string(root, "message");
            JsonObject summaryJson = object(root, "summary");
            RevisionSummary summary = new RevisionSummary(integer(summaryJson, "added"),
                    integer(summaryJson, "modified"), integer(summaryJson, "removed"));
            OptionalLong rollbackOf = root.has("rollbackOf")
                    ? OptionalLong.of(root.get("rollbackOf").getAsLong())
                    : OptionalLong.empty();
            String snapshotSha256 = string(root, "snapshotSha256");
            JsonObject snapshotJson = object(root, "snapshot");
            WorkspaceDecodeResult workspaceResult = this.workspaceCodec.decode(snapshotJson);
            if (workspaceResult instanceof WorkspaceDecodeResult.Failure failure) {
                return failure("revision.snapshot." + failure.code(), "snapshot." + failure.fieldPath(),
                        failure.message());
            }
            WorkspaceSnapshot snapshot = ((WorkspaceDecodeResult.Success) workspaceResult).snapshot();
            // Integrity covers the stored JSON shape, not its compatibility-migrated model form.
            String actualSnapshotHash = ContentHashes.sha256(this.workspaceCodec.encodeJson(snapshotJson));
            if (!actualSnapshotHash.equals(snapshotSha256)) {
                return failure("revision.snapshot.checksum", "snapshotSha256",
                        "Snapshot checksum does not match its content");
            }

            JsonArray artifactArray = array(root, "artifacts");
            List<RenderedArtifact> artifacts = new ArrayList<>();
            for (int index = 0; index < artifactArray.size(); index++) {
                JsonElement element = artifactArray.get(index);
                if (!element.isJsonObject()) {
                    return failure("revision.artifact.invalid", "artifacts[" + index + "]",
                            "Artifact must be an object");
                }
                JsonObject encoded = element.getAsJsonObject();
                ArtifactPath path = ArtifactPath.of(string(encoded, "path"));
                byte[] bytes = string(encoded, "content").getBytes(StandardCharsets.UTF_8);
                artifacts.add(new RenderedArtifact(path, bytes, string(encoded, "sha256")));
            }
            JsonArray deletedArray = array(root, "deletedPaths");
            List<ArtifactPath> deletedPaths = new ArrayList<>();
            deletedArray.forEach(element -> deletedPaths.add(ArtifactPath.of(element.getAsString())));
            return new RevisionDecodeResult.Success(new Revision(version, parentVersion, committedAt,
                    message, summary, rollbackOf, snapshot, snapshotSha256,
                    RenderPlan.of(artifacts), deletedPaths));
        } catch (RuntimeException exception) {
            return failure("revision.json.invalid", "$", safeMessage(exception));
        }
    }

    private static RevisionDecodeResult.Failure failure(String code, String path, String message) {
        return new RevisionDecodeResult.Failure(code, path, message == null ? code : message);
    }

    private static String string(JsonObject parent, String name) {
        return required(parent, name).getAsString();
    }

    private static int integer(JsonObject parent, String name) {
        return required(parent, name).getAsInt();
    }

    private static long longValue(JsonObject parent, String name) {
        return required(parent, name).getAsLong();
    }

    private static JsonObject object(JsonObject parent, String name) {
        JsonElement value = required(parent, name);
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException(name + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonObject parent, String name) {
        JsonElement value = required(parent, name);
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException(name + " must be an array");
        }
        return value.getAsJsonArray();
    }

    private static JsonElement required(JsonObject parent, String name) {
        if (!parent.has(name) || parent.get(name).isJsonNull()) {
            throw new IllegalArgumentException("Missing required field " + name);
        }
        return parent.get(name);
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
