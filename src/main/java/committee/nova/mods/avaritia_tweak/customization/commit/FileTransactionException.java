package committee.nova.mods.avaritia_tweak.customization.commit;

public final class FileTransactionException extends Exception {
    private final boolean recoveryFailure;

    public FileTransactionException(String message, boolean recoveryFailure, Throwable cause) {
        super(message, cause);
        this.recoveryFailure = recoveryFailure;
    }

    public boolean recoveryFailure() {
        return this.recoveryFailure;
    }
}
