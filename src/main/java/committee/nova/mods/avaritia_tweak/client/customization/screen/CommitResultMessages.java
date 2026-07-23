package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;

final class CommitResultMessages {
    private CommitResultMessages() {
    }

    static String describe(CommitResult result) {
        if (result instanceof CommitResult.ValidationFailed validation) {
            return validation.report().errors().isEmpty() ? "Validation failed"
                    : validation.report().errors().get(0).fieldPath() + ": "
                    + validation.report().errors().get(0).messageKey();
        }
        if (result instanceof CommitResult.VersionConflict conflict) {
            return "Version conflict: expected " + conflict.expectedVersion()
                    + ", actual " + conflict.actualVersion();
        }
        if (result instanceof CommitResult.PreviewMismatch) {
            return "Preview changed; generate a new preview";
        }
        if (result instanceof CommitResult.TargetBusy busy) {
            return busy.message();
        }
        if (result instanceof CommitResult.IoFailure failure) {
            return failure.code() + ": " + failure.message();
        }
        if (result instanceof CommitResult.RecoveryFailure failure) {
            return "Recovery failed: " + failure.message();
        }
        return result.toString();
    }

    static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }
}
