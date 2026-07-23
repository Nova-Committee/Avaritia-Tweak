package committee.nova.mods.avaritia_tweak.customization.commit;

import java.util.concurrent.CompletionStage;

public interface CustomizationCommitTarget {
    CompletionStage<CommitResult> commit(CommitRequest request);
}
