package committee.nova.mods.avaritia_tweak.client.customization.persistence;

import java.nio.file.Path;
import java.util.Objects;

public sealed interface DraftLoadResult
        permits DraftLoadResult.Missing, DraftLoadResult.Loaded, DraftLoadResult.Corrupt {
    record Missing(DraftState fallback) implements DraftLoadResult {
        public Missing {
            Objects.requireNonNull(fallback, "fallback");
        }
    }

    record Loaded(DraftState draft) implements DraftLoadResult {
        public Loaded {
            Objects.requireNonNull(draft, "draft");
        }
    }

    record Corrupt(DraftState fallback, Path preservedCopy, DraftDecodeResult.Failure failure)
            implements DraftLoadResult {
        public Corrupt {
            Objects.requireNonNull(fallback, "fallback");
            Objects.requireNonNull(preservedCopy, "preservedCopy");
            Objects.requireNonNull(failure, "failure");
        }
    }
}
