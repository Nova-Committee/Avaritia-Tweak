package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.validation.ValidationReport;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;

public sealed interface CommitResult permits CommitResult.Success, CommitResult.ValidationFailed,
        CommitResult.VersionConflict, CommitResult.PreviewMismatch, CommitResult.ExternalFileConflict,
        CommitResult.TargetBusy, CommitResult.IoFailure, CommitResult.RecoveryFailure {

    record Success(Revision revision) implements CommitResult {
        public Success {
            Objects.requireNonNull(revision, "revision");
        }
    }

    record ValidationFailed(ValidationReport report) implements CommitResult {
        public ValidationFailed {
            Objects.requireNonNull(report, "report");
        }
    }

    record VersionConflict(long expectedVersion, long actualVersion) implements CommitResult {
    }

    record PreviewMismatch(SortedMap<ArtifactPath, String> expected,
                           SortedMap<ArtifactPath, String> actual) implements CommitResult {
        public PreviewMismatch {
            expected = java.util.Collections.unmodifiableSortedMap(new java.util.TreeMap<>(expected));
            actual = java.util.Collections.unmodifiableSortedMap(new java.util.TreeMap<>(actual));
        }
    }

    record ExternalFileConflict(List<FileConflict> conflicts) implements CommitResult {
        public ExternalFileConflict {
            conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts"));
        }
    }

    record TargetBusy(String message) implements CommitResult {
        public TargetBusy {
            Objects.requireNonNull(message, "message");
        }
    }

    record IoFailure(String code, Optional<ArtifactPath> logicalPath, String message) implements CommitResult {
        public IoFailure {
            Objects.requireNonNull(code, "code");
            logicalPath = Objects.requireNonNull(logicalPath, "logicalPath");
            Objects.requireNonNull(message, "message");
        }
    }

    record RecoveryFailure(String message) implements CommitResult {
        public RecoveryFailure {
            Objects.requireNonNull(message, "message");
        }
    }
}
