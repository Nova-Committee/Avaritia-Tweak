package committee.nova.mods.avaritia_tweak.customization.model;

import java.util.List;
import java.util.Objects;

/** Bridges the editor's three physical addition slots with Avaritia's single addition matcher. */
public final class ExtremeSmithingInputs {
    public static final int ADDITION_SLOT_COUNT = 3;

    private ExtremeSmithingInputs() {
    }

    public static List<IngredientSpec> validateAdditions(List<IngredientSpec> additions) {
        Objects.requireNonNull(additions, "additions");
        if (additions.size() != ADDITION_SLOT_COUNT) {
            throw new IllegalArgumentException("Extreme smithing requires exactly three addition inputs");
        }
        additions.forEach(addition -> Objects.requireNonNull(addition, "addition"));
        return List.copyOf(additions);
    }

    /**
     * Expands the serialized matcher into the three physical slots exposed by Avaritia's UI.
     * Official recipes encode their three slot items as a three-way choice; generic matchers are
     * repeated because the runtime applies the same matcher to every addition slot.
     */
    public static List<IngredientSpec> expandSerializedAddition(IngredientSpec addition) {
        Objects.requireNonNull(addition, "addition");
        if (addition instanceof IngredientSpec.Choice choice
                && choice.alternatives().size() == ADDITION_SLOT_COUNT) {
            return List.copyOf(choice.alternatives());
        }
        return List.of(addition, addition, addition);
    }

    /** Collapses the three editor slots to the matcher required by Avaritia's recipe API. */
    public static IngredientSpec serializedAddition(List<IngredientSpec> additions) {
        List<IngredientSpec> validated = validateAdditions(additions);
        IngredientSpec first = validated.get(0);
        if (validated.stream().allMatch(first::equals)) {
            return first;
        }
        return new IngredientSpec.Choice(validated);
    }
}
