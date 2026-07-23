package committee.nova.mods.avaritia_tweak.customization.diff;

import java.util.Objects;
import java.util.Optional;

public record FieldChange(String fieldPath, Optional<String> before, Optional<String> after) {
    public FieldChange {
        Objects.requireNonNull(fieldPath, "fieldPath");
        before = Objects.requireNonNull(before, "before");
        after = Objects.requireNonNull(after, "after");
    }
}
