package committee.nova.mods.avaritia_tweak.customization.diff;

import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record EntryChange(ChangeType type, EntryKey key,
                          Optional<CustomizationEntry> before,
                          Optional<CustomizationEntry> after,
                          List<FieldChange> fields) {
    public EntryChange {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        before = Objects.requireNonNull(before, "before");
        after = Objects.requireNonNull(after, "after");
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    }
}
