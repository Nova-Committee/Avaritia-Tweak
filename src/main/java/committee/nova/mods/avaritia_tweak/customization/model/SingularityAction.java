package committee.nova.mods.avaritia_tweak.customization.model;

public enum SingularityAction {
    REMOVE(true),
    REMOVE_ALL(false),
    REMOVE_RECIPE(true),
    REMOVE_ALL_RECIPES(false);

    private final boolean requiresTarget;

    SingularityAction(boolean requiresTarget) {
        this.requiresTarget = requiresTarget;
    }

    public boolean requiresTarget() {
        return this.requiresTarget;
    }
}
