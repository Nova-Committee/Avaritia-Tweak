package committee.nova.mods.avaritia_tweak.customization.model;

public enum EntryKind {
    SHAPED_TABLE(true),
    SHAPELESS_TABLE(true),
    COMPRESSOR(true),
    EXTREME_SMITHING(true),
    INFINITY_CATALYST(true),
    ETERNAL_SINGULARITY(true),
    SINGULARITY_DEFINITION(false),
    SINGULARITY_OPERATION(false);

    private final boolean recipe;

    EntryKind(boolean recipe) {
        this.recipe = recipe;
    }

    public boolean isRecipe() {
        return this.recipe;
    }
}
