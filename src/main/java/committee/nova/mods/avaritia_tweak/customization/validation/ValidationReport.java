package committee.nova.mods.avaritia_tweak.customization.validation;

import java.util.List;
import java.util.Objects;

public record ValidationReport(List<ValidationIssue> issues) {
    public ValidationReport {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public boolean isValid() {
        return this.issues.stream().noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }

    public List<ValidationIssue> errors() {
        return this.issues.stream()
                .filter(issue -> issue.severity() == ValidationSeverity.ERROR)
                .toList();
    }

    public List<ValidationIssue> warnings() {
        return this.issues.stream()
                .filter(issue -> issue.severity() == ValidationSeverity.WARNING)
                .toList();
    }
}
