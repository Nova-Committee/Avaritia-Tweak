package committee.nova.mods.avaritia_tweak.customization.validation;

import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ValidationIssue(ValidationSeverity severity, Optional<EntryKey> entryKey,
                              String fieldPath, String messageKey, List<String> arguments) {
    public ValidationIssue {
        Objects.requireNonNull(severity, "severity");
        entryKey = Objects.requireNonNull(entryKey, "entryKey");
        Objects.requireNonNull(fieldPath, "fieldPath");
        Objects.requireNonNull(messageKey, "messageKey");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public static ValidationIssue error(EntryKey entryKey, String fieldPath,
                                        String messageKey, String... arguments) {
        return new ValidationIssue(ValidationSeverity.ERROR, Optional.ofNullable(entryKey),
                fieldPath, messageKey, List.of(arguments));
    }

    public static ValidationIssue warning(EntryKey entryKey, String fieldPath,
                                          String messageKey, String... arguments) {
        return new ValidationIssue(ValidationSeverity.WARNING, Optional.ofNullable(entryKey),
                fieldPath, messageKey, List.of(arguments));
    }
}
