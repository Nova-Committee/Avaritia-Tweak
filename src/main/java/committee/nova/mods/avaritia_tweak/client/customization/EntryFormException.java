package committee.nova.mods.avaritia_tweak.client.customization;

public final class EntryFormException extends Exception {
    private final String fieldPath;

    public EntryFormException(String fieldPath, String message) {
        super(message);
        this.fieldPath = fieldPath;
    }

    public String fieldPath() {
        return this.fieldPath;
    }
}
