package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.SingularityAction;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public final class EntryForm {
    private EntryKind kind;
    private String idText;
    private String note;
    private OutputTarget target;
    private CraftingTier tier;
    private final TreeMap<Integer, IngredientSpec> grid = new TreeMap<>();
    private final ArrayList<IngredientSpec> ingredients = new ArrayList<>();
    private IngredientSpec ingredient;
    private IngredientSpec template;
    private IngredientSpec base;
    private IngredientSpec addition;
    private ItemStackSpec result;
    private int inputCount;
    private int timeCost;
    private int count;
    private String group;
    private String displayName;
    private int overlayColor;
    private int underlayColor;
    private boolean enabled;
    private boolean recipeEnabled;
    private SingularityAction action;
    private String singularityIdText;

    private EntryForm(EntryKind kind) {
        ResourceLocation stone = ResourceLocation.tryParse("minecraft:stone");
        this.kind = kind;
        this.idText = "avaritia_tweak:new_entry";
        this.note = "";
        this.target = OutputTarget.KUBEJS;
        this.tier = CraftingTier.EXTREME;
        this.ingredient = new IngredientSpec.Item(stone);
        this.template = new IngredientSpec.Item(stone);
        this.base = new IngredientSpec.Item(stone);
        this.addition = new IngredientSpec.Item(stone);
        this.result = new ItemStackSpec(ResourceLocation.tryParse("minecraft:diamond"), 1);
        this.inputCount = 1000;
        this.timeCost = 240;
        this.count = 1;
        this.group = "default";
        this.displayName = "singularity.avaritia_tweak.new_entry";
        this.overlayColor = 0x3b2754;
        this.underlayColor = 0x3b2754;
        this.enabled = true;
        this.recipeEnabled = true;
        this.action = SingularityAction.REMOVE;
        this.singularityIdText = "avaritia:coal";
    }

    public static EntryForm newEntry(EntryKind kind) {
        return new EntryForm(kind);
    }

    public static EntryForm from(CustomizationEntry entry) {
        EntryForm form = new EntryForm(entry.kind());
        form.idText = entry.id().toString();
        form.note = entry.note();
        form.target = entry.target();
        if (entry instanceof CustomizationEntry.ShapedTable shaped) {
            form.tier = shaped.tier();
            form.grid.putAll(shaped.ingredients());
            form.result = shaped.result();
        } else if (entry instanceof CustomizationEntry.NoConsumeCatalystShaped shaped) {
            form.tier = shaped.tier();
            form.grid.putAll(shaped.ingredients());
            form.result = shaped.result();
        } else if (entry instanceof CustomizationEntry.ShapelessTable shapeless) {
            form.tier = shapeless.tier();
            form.ingredients.addAll(shapeless.ingredients());
            form.result = shapeless.result();
        } else if (entry instanceof CustomizationEntry.Compressor compressor) {
            form.ingredient = compressor.ingredient();
            form.result = compressor.result();
            form.inputCount = compressor.inputCount();
            form.timeCost = compressor.timeCost();
        } else if (entry instanceof CustomizationEntry.ExtremeSmithing smithing) {
            form.template = smithing.template();
            form.base = smithing.base();
            form.addition = smithing.addition();
            form.result = smithing.result();
        } else if (entry instanceof CustomizationEntry.InfinityCatalyst catalyst) {
            form.group = catalyst.group();
            form.ingredients.addAll(catalyst.ingredients());
            form.count = catalyst.count();
        } else if (entry instanceof CustomizationEntry.EternalSingularity eternal) {
            form.ingredients.addAll(eternal.ingredients());
            form.count = eternal.count();
        } else if (entry instanceof CustomizationEntry.SingularityDefinition definition) {
            form.displayName = definition.displayName();
            form.overlayColor = definition.overlayColor();
            form.underlayColor = definition.underlayColor();
            form.count = definition.count();
            form.timeCost = definition.timeCost();
            form.ingredient = definition.ingredient();
            form.enabled = definition.enabled();
            form.recipeEnabled = definition.recipeEnabled();
        } else if (entry instanceof CustomizationEntry.SingularityOperation operation) {
            form.action = operation.action();
            form.singularityIdText = operation.singularityId().map(Object::toString).orElse("");
        }
        return form;
    }

    public CustomizationEntry build() throws EntryFormException {
        ResourceLocation id = ResourceLocation.tryParse(this.idText);
        if (id == null) {
            throw new EntryFormException("id", "Invalid resource ID");
        }
        return switch (this.kind) {
            case SHAPED_TABLE -> new CustomizationEntry.ShapedTable(id, this.target, this.note, this.tier,
                    this.grid, this.result);
            case NO_CONSUME_CATALYST_SHAPED -> new CustomizationEntry.NoConsumeCatalystShaped(
                    id, this.target, this.note, this.tier, this.grid, this.result);
            case SHAPELESS_TABLE -> new CustomizationEntry.ShapelessTable(id, this.target, this.note, this.tier,
                    this.ingredients, this.result);
            case COMPRESSOR -> new CustomizationEntry.Compressor(id, this.target, this.note, this.ingredient,
                    this.result, this.inputCount, this.timeCost);
            case EXTREME_SMITHING -> new CustomizationEntry.ExtremeSmithing(id, this.target, this.note,
                    this.template, this.base, this.addition, this.result);
            case INFINITY_CATALYST -> new CustomizationEntry.InfinityCatalyst(id, this.target, this.note,
                    this.group, this.ingredients, this.count);
            case ETERNAL_SINGULARITY -> new CustomizationEntry.EternalSingularity(id, this.target, this.note,
                    this.ingredients, this.count);
            case SINGULARITY_DEFINITION -> new CustomizationEntry.SingularityDefinition(id, this.target, this.note,
                    this.displayName, this.overlayColor, this.underlayColor, this.count, this.timeCost,
                    this.ingredient, this.enabled, this.recipeEnabled);
            case SINGULARITY_OPERATION -> {
                Optional<ResourceLocation> singularityId = Optional.empty();
                if (this.action.requiresTarget()) {
                    ResourceLocation parsed = ResourceLocation.tryParse(this.singularityIdText);
                    if (parsed == null) {
                        throw new EntryFormException("singularityId", "Invalid singularity resource ID");
                    }
                    singularityId = Optional.of(parsed);
                }
                yield new CustomizationEntry.SingularityOperation(
                        id, this.target, this.note, this.action, singularityId);
            }
        };
    }

    public EntryKind kind() {
        return this.kind;
    }

    public String idText() {
        return this.idText;
    }

    public void idText(String idText) {
        this.idText = idText;
    }

    public String note() {
        return this.note;
    }

    public void note(String note) {
        this.note = note;
    }

    public OutputTarget target() {
        return this.target;
    }

    public void target(OutputTarget target) {
        this.target = target.supports(this.kind)
                ? target
                : OutputTarget.compatibleWith(this.kind).get(0);
    }

    public CraftingTier tier() {
        return this.tier;
    }

    public void tier(CraftingTier tier) {
        this.tier = tier;
        this.grid.keySet().removeIf(slot -> slot >= tier.capacity());
        while (this.ingredients.size() > tier.capacity()) {
            this.ingredients.remove(this.ingredients.size() - 1);
        }
    }

    public SortedMap<Integer, IngredientSpec> grid() {
        return java.util.Collections.unmodifiableSortedMap(this.grid);
    }

    public void gridIngredient(int slot, Optional<IngredientSpec> value) {
        value.ifPresentOrElse(ingredient -> this.grid.put(slot, ingredient), () -> this.grid.remove(slot));
    }

    public List<IngredientSpec> ingredients() {
        return List.copyOf(this.ingredients);
    }

    public void ingredientAt(int index, IngredientSpec value) {
        this.ingredients.set(index, value);
    }

    public void addIngredient(IngredientSpec value) {
        this.ingredients.add(value);
    }

    public void removeIngredient(int index) {
        this.ingredients.remove(index);
    }

    public IngredientSpec ingredient() {
        return this.ingredient;
    }

    public void ingredient(IngredientSpec ingredient) {
        this.ingredient = ingredient;
    }

    public IngredientSpec template() {
        return this.template;
    }

    public void template(IngredientSpec template) {
        this.template = template;
    }

    public IngredientSpec base() {
        return this.base;
    }

    public void base(IngredientSpec base) {
        this.base = base;
    }

    public IngredientSpec addition() {
        return this.addition;
    }

    public void addition(IngredientSpec addition) {
        this.addition = addition;
    }

    public ItemStackSpec result() {
        return this.result;
    }

    public void result(ItemStackSpec result) {
        this.result = result;
    }

    public int inputCount() {
        return this.inputCount;
    }

    public void inputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public int timeCost() {
        return this.timeCost;
    }

    public void timeCost(int timeCost) {
        this.timeCost = timeCost;
    }

    public int count() {
        return this.count;
    }

    public void count(int count) {
        this.count = count;
    }

    public String group() {
        return this.group;
    }

    public void group(String group) {
        this.group = group;
    }

    public String displayName() {
        return this.displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName;
    }

    public int overlayColor() {
        return this.overlayColor;
    }

    public void overlayColor(int overlayColor) {
        this.overlayColor = overlayColor;
    }

    public int underlayColor() {
        return this.underlayColor;
    }

    public void underlayColor(int underlayColor) {
        this.underlayColor = underlayColor;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean recipeEnabled() {
        return this.recipeEnabled;
    }

    public void recipeEnabled(boolean recipeEnabled) {
        this.recipeEnabled = recipeEnabled;
    }

    public SingularityAction action() {
        return this.action;
    }

    public void action(SingularityAction action) {
        this.action = action;
        if (!action.requiresTarget()) {
            this.singularityIdText = "";
        }
    }

    public String singularityIdText() {
        return this.singularityIdText;
    }

    public void singularityIdText(String singularityIdText) {
        this.singularityIdText = singularityIdText;
    }
}
