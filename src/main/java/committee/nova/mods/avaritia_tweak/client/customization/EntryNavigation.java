package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Pure navigation operations shared by the editor screen and its tests. */
public final class EntryNavigation {
    private EntryNavigation() {
    }

    public static List<CustomizationEntry> search(Collection<CustomizationEntry> entries, String query) {
        Objects.requireNonNull(entries, "entries");
        String normalized = Objects.requireNonNull(query, "query").strip().toLowerCase(Locale.ROOT);
        return entries.stream()
                .filter(entry -> matches(entry, normalized))
                .sorted((left, right) -> left.key().compareTo(right.key()))
                .toList();
    }

    public static EntryForm duplicateForEditing(CustomizationEntry source,
                                                Collection<CustomizationEntry> existingEntries) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(existingEntries, "existingEntries");
        EntryForm copy = EntryForm.from(source);
        copy.idText(nextCopyId(source.id(), existingEntries).toString());
        return copy;
    }

    private static boolean matches(CustomizationEntry entry, String normalized) {
        if (normalized.isEmpty()) {
            return true;
        }
        return entry.id().toString().toLowerCase(Locale.ROOT).contains(normalized)
                || entry.note().toLowerCase(Locale.ROOT).contains(normalized)
                || entry.kind().name().toLowerCase(Locale.ROOT).contains(normalized)
                || entry.target().name().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static ResourceLocation nextCopyId(ResourceLocation source,
                                               Collection<CustomizationEntry> existingEntries) {
        Set<ResourceLocation> usedIds = new HashSet<>();
        existingEntries.forEach(entry -> usedIds.add(entry.id()));
        String basePath = source.getPath() + "_copy";
        int suffix = 1;
        while (true) {
            String path = suffix == 1 ? basePath : basePath + "_" + suffix;
            ResourceLocation candidate = ResourceLocation.tryParse(source.getNamespace() + ":" + path);
            if (candidate == null) {
                throw new IllegalArgumentException("Unable to create a copy ID for " + source);
            }
            if (!usedIds.contains(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }
}
