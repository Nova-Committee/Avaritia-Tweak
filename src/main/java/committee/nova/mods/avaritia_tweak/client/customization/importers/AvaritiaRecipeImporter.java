package committee.nova.mods.avaritia_tweak.client.customization.importers;

import committee.nova.mods.avaritia.common.crafting.recipe.CompressorRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.EternalSingularityCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.InfinityCatalystCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.NoConsumeCatalystShapedRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

public final class AvaritiaRecipeImporter {
    private final IngredientImporter ingredientImporter;
    private final ItemStackReader itemStacks;
    private final SpecialRecipeReader specialRecipes;

    public AvaritiaRecipeImporter() {
        this(new IngredientImporter());
    }

    public AvaritiaRecipeImporter(IngredientImporter ingredientImporter) {
        this(ingredientImporter, AvaritiaRecipeImporter::readMinecraftItemStack,
                new NetworkSpecialRecipeReader());
    }

    AvaritiaRecipeImporter(IngredientImporter ingredientImporter,
                           ItemStackReader itemStacks, SpecialRecipeReader specialRecipes) {
        this.ingredientImporter = ingredientImporter;
        this.itemStacks = itemStacks;
        this.specialRecipes = specialRecipes;
    }

    public boolean supports(Recipe<?> recipe) {
        return recipe instanceof ShapedTableCraftingRecipe
                || recipe instanceof ShapelessTableCraftingRecipe
                || recipe instanceof CompressorRecipe
                || recipe instanceof ExtremeSmithingRecipe;
    }

    public RecipeImportResult importRecipe(Recipe<?> recipe, OutputTarget target,
                                           RegistryAccess registryAccess) {
        if (target == OutputTarget.DATAPACK) {
            return failure(recipe, "target", "recipe.target.unsupported",
                    "Recipes can only target KubeJS or CraftTweaker");
        }
        try {
            if (recipe instanceof InfinityCatalystCraftRecipe catalyst) {
                return importCatalyst(catalyst, target);
            }
            if (recipe instanceof EternalSingularityCraftRecipe eternal) {
                return importEternal(eternal, target);
            }
            if (recipe instanceof NoConsumeCatalystShapedRecipe shaped) {
                if (target != OutputTarget.KUBEJS) {
                    return failure(recipe, "target", "recipe.target.unsupported",
                            "Catalyst-preserving shaped recipes can only target KubeJS");
                }
                return importShaped(shaped, target, registryAccess, true);
            }
            if (recipe instanceof ShapedTableCraftingRecipe shaped) {
                return importShaped(shaped, target, registryAccess, false);
            }
            if (recipe instanceof ShapelessTableCraftingRecipe shapeless) {
                return importShapeless(shapeless, target, registryAccess);
            }
            if (recipe instanceof CompressorRecipe compressor) {
                IngredientImportResult input = this.ingredientImporter.importIngredient(
                        compressor.getInput(), "ingredient");
                if (input instanceof IngredientImportResult.Failure failure) {
                    return failure(recipe, failure);
                }
                return new RecipeImportResult.Success(new CustomizationEntry.Compressor(recipe.getId(), target,
                        ((IngredientImportResult.Success) input).ingredient(),
                        itemStack(compressor.getResultItem(registryAccess)),
                        compressor.getInputCount(), compressor.getTimeCost()));
            }
            if (recipe instanceof ExtremeSmithingRecipe smithing) {
                ImportedIngredients imported = importIngredients(recipe,
                        List.of(smithing.template, smithing.base, smithing.additions),
                        List.of("template", "base", "addition"));
                if (imported.failure != null) {
                    return imported.failure;
                }
                return new RecipeImportResult.Success(new CustomizationEntry.ExtremeSmithing(recipe.getId(), target,
                        imported.ingredients.get(0), imported.ingredients.get(1), imported.ingredients.get(2),
                        itemStack(smithing.getResultItem(registryAccess))));
            }
            return failure(recipe, "kind", "recipe.kind.unsupported",
                    "Only Avaritia 1.4.1 visual-editor recipe types are supported");
        } catch (RuntimeException exception) {
            return failure(recipe, "$", "recipe.import.failed",
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    public RecipeImportResult importSingularity(Singularity singularity, OutputTarget target) {
        ResourceLocation id = singularity.getRegistryName();
        IngredientImportResult imported = this.ingredientImporter.importIngredient(
                singularity.getIngredient(), "ingredient");
        if (imported instanceof IngredientImportResult.Failure failure) {
            return new RecipeImportResult.Failure(id, failure.fieldPath(), failure.code(), failure.message());
        }
        return new RecipeImportResult.Success(new CustomizationEntry.SingularityDefinition(
                id, target, singularity.getDisplayName(), singularity.getOverlayColor(),
                singularity.getUnderlayColor(), singularity.getRealCount(), singularity.getTimeCost(),
                ((IngredientImportResult.Success) imported).ingredient(),
                singularity.isEnabled(), singularity.isRecipeEnabled()));
    }

    private RecipeImportResult importShaped(ShapedTableCraftingRecipe recipe, OutputTarget target,
                                            RegistryAccess registryAccess, boolean catalystPreserving) {
        Optional<CraftingTier> tier = CraftingTier.fromValue(recipe.getTier());
        if (tier.isEmpty()) {
            return failure(recipe, "tier", "recipe.tier.unsupported", "Unsupported table tier");
        }
        TreeMap<Integer, IngredientSpec> grid = new TreeMap<>();
        NonNullList<Ingredient> encoded = recipe.getIngredients();
        for (int row = 0; row < recipe.getHeight(); row++) {
            for (int column = 0; column < recipe.getWidth(); column++) {
                int sourceIndex = row * recipe.getWidth() + column;
                Ingredient ingredient = encoded.get(sourceIndex);
                if (ingredient.isEmpty()) {
                    continue;
                }
                String field = "ingredients[" + sourceIndex + "]";
                IngredientImportResult imported = this.ingredientImporter.importIngredient(ingredient, field);
                if (imported instanceof IngredientImportResult.Failure failure) {
                    return failure(recipe, failure);
                }
                grid.put(row * tier.get().gridSize() + column,
                        ((IngredientImportResult.Success) imported).ingredient());
            }
        }
        ItemStackSpec result = itemStack(recipe.getResultItem(registryAccess));
        CustomizationEntry entry = catalystPreserving
                ? new CustomizationEntry.NoConsumeCatalystShaped(recipe.getId(), target, tier.get(), grid, result)
                : new CustomizationEntry.ShapedTable(recipe.getId(), target, tier.get(), grid, result);
        return new RecipeImportResult.Success(entry);
    }

    private RecipeImportResult importShapeless(ShapelessTableCraftingRecipe recipe, OutputTarget target,
                                               RegistryAccess registryAccess) {
        Optional<CraftingTier> tier = CraftingTier.fromValue(recipe.getTier());
        if (tier.isEmpty()) {
            return failure(recipe, "tier", "recipe.tier.unsupported", "Unsupported table tier");
        }
        ImportedIngredients imported = importIngredients(recipe, recipe.getIngredients(), null);
        if (imported.failure != null) {
            return imported.failure;
        }
        return new RecipeImportResult.Success(new CustomizationEntry.ShapelessTable(recipe.getId(), target,
                tier.get(), imported.ingredients, itemStack(recipe.getResultItem(registryAccess))));
    }

    private RecipeImportResult importCatalyst(InfinityCatalystCraftRecipe recipe, OutputTarget target) {
        DecodedSpecial decoded = this.specialRecipes.catalyst(recipe);
        ImportedIngredients imported = importIngredients(recipe, decoded.ingredients, null);
        if (imported.failure != null) {
            return imported.failure;
        }
        return new RecipeImportResult.Success(new CustomizationEntry.InfinityCatalyst(recipe.getId(), target,
                decoded.group, imported.ingredients, decoded.count));
    }

    private RecipeImportResult importEternal(EternalSingularityCraftRecipe recipe, OutputTarget target) {
        DecodedSpecial decoded = this.specialRecipes.eternal(recipe);
        ImportedIngredients imported = importIngredients(recipe, decoded.ingredients, null);
        if (imported.failure != null) {
            return imported.failure;
        }
        return new RecipeImportResult.Success(new CustomizationEntry.EternalSingularity(recipe.getId(), target,
                imported.ingredients, decoded.count));
    }

    private ImportedIngredients importIngredients(Recipe<?> recipe, List<Ingredient> encoded,
                                                  List<String> fieldNames) {
        List<IngredientSpec> result = new ArrayList<>();
        for (int index = 0; index < encoded.size(); index++) {
            String field = fieldNames == null ? "ingredients[" + index + "]" : fieldNames.get(index);
            IngredientImportResult imported = this.ingredientImporter.importIngredient(encoded.get(index), field);
            if (imported instanceof IngredientImportResult.Failure failure) {
                return new ImportedIngredients(List.of(), failure(recipe, failure));
            }
            result.add(((IngredientImportResult.Success) imported).ingredient());
        }
        return new ImportedIngredients(List.copyOf(result), null);
    }

    private ItemStackSpec itemStack(ItemStack stack) {
        return this.itemStacks.read(stack);
    }

    private static ItemStackSpec readMinecraftItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Recipe output is empty");
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            throw new IllegalArgumentException("Recipe output item is not registered");
        }
        return new ItemStackSpec(id, stack.getCount(), Optional.ofNullable(stack.getTag()));
    }

    private static DecodedSpecial decodeCatalystNetwork(InfinityCatalystCraftRecipe recipe) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new InfinityCatalystCraftRecipe.Serializer().toNetwork(buffer, recipe);
            String group = buffer.readUtf();
            int size = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                ingredients.add(Ingredient.fromNetwork(buffer));
            }
            return new DecodedSpecial(group, ingredients, buffer.readInt());
        } finally {
            buffer.release();
        }
    }

    private static DecodedSpecial decodeEternalNetwork(EternalSingularityCraftRecipe recipe) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new EternalSingularityCraftRecipe.Serializer().toNetwork(buffer, recipe);
            int size = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                ingredients.add(Ingredient.fromNetwork(buffer));
            }
            return new DecodedSpecial("", ingredients, buffer.readInt());
        } finally {
            buffer.release();
        }
    }

    private static RecipeImportResult.Failure failure(Recipe<?> recipe,
                                                       IngredientImportResult.Failure failure) {
        return failure(recipe, failure.fieldPath(), failure.code(), failure.message());
    }

    private static RecipeImportResult.Failure failure(Recipe<?> recipe, String field,
                                                       String code, String message) {
        return new RecipeImportResult.Failure(recipe.getId(), field, code, message);
    }

    @FunctionalInterface
    interface ItemStackReader {
        ItemStackSpec read(ItemStack stack);
    }

    interface SpecialRecipeReader {
        DecodedSpecial catalyst(InfinityCatalystCraftRecipe recipe);

        DecodedSpecial eternal(EternalSingularityCraftRecipe recipe);
    }

    record DecodedSpecial(String group, List<Ingredient> ingredients, int count) {
        DecodedSpecial {
            ingredients = List.copyOf(ingredients);
        }
    }

    private static final class NetworkSpecialRecipeReader implements SpecialRecipeReader {
        @Override
        public DecodedSpecial catalyst(InfinityCatalystCraftRecipe recipe) {
            return decodeCatalystNetwork(recipe);
        }

        @Override
        public DecodedSpecial eternal(EternalSingularityCraftRecipe recipe) {
            return decodeEternalNetwork(recipe);
        }
    }

    private record ImportedIngredients(List<IngredientSpec> ingredients,
                                       RecipeImportResult.Failure failure) {
    }
}
