package committee.nova.mods.avaritia_tweak.client.customization.importers;

import committee.nova.mods.avaritia.common.crafting.recipe.CompressorRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.EternalSingularityCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.InfinityCatalystCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia_tweak.customization.minecraft.MinecraftItemStacks;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;

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
        this(ingredientImporter, MinecraftItemStacks::toSpec, new NetworkSpecialRecipeReader());
    }

    AvaritiaRecipeImporter(IngredientImporter ingredientImporter,
                           ItemStackReader itemStacks, SpecialRecipeReader specialRecipes) {
        this.ingredientImporter = ingredientImporter;
        this.itemStacks = itemStacks;
        this.specialRecipes = specialRecipes;
    }

    public boolean supports(RecipeHolder<?> holder) {
        return supports(holder.value());
    }

    boolean supports(Recipe<?> recipe) {
        return recipe instanceof ShapedTableCraftingRecipe
                || recipe instanceof ShapelessTableCraftingRecipe
                || recipe instanceof CompressorRecipe
                || recipe instanceof ExtremeSmithingRecipe;
    }

    public RecipeImportResult importRecipe(RecipeHolder<?> holder, OutputTarget target,
                                           RegistryAccess registryAccess) {
        return importRecipe(holder.id(), holder.value(), target, registryAccess);
    }

    RecipeImportResult importRecipe(ResourceLocation recipeId, Recipe<?> recipe, OutputTarget target,
                                    RegistryAccess registryAccess) {
        if (target == OutputTarget.DATAPACK) {
            return failure(recipeId, "target", "recipe.target.unsupported",
                    "Recipes can only target KubeJS or CraftTweaker");
        }
        try {
            if (recipe instanceof InfinityCatalystCraftRecipe catalyst) {
                return importCatalyst(recipeId, catalyst, target, registryAccess);
            }
            if (recipe instanceof EternalSingularityCraftRecipe eternal) {
                return importEternal(recipeId, eternal, target, registryAccess);
            }
            if (recipe instanceof ShapedTableCraftingRecipe shaped) {
                return importShaped(recipeId, shaped, target, registryAccess);
            }
            if (recipe instanceof ShapelessTableCraftingRecipe shapeless) {
                return importShapeless(recipeId, shapeless, target, registryAccess);
            }
            if (recipe instanceof CompressorRecipe compressor) {
                IngredientImportResult input = this.ingredientImporter.importIngredient(
                        compressor.getInput(), "ingredient");
                if (input instanceof IngredientImportResult.Failure importFailure) {
                    return failure(recipeId, importFailure);
                }
                return new RecipeImportResult.Success(new CustomizationEntry.Compressor(recipeId, target,
                        ((IngredientImportResult.Success) input).ingredient(),
                        itemStack(compressor.getResultItem(registryAccess)),
                        compressor.getInputCount(), compressor.getTimeCost()));
            }
            if (recipe instanceof ExtremeSmithingRecipe smithing) {
                ImportedIngredients imported = importIngredients(recipeId,
                        List.of(smithing.template, smithing.base, smithing.additions),
                        List.of("template", "base", "addition"));
                if (imported.failure != null) {
                    return imported.failure;
                }
                return new RecipeImportResult.Success(new CustomizationEntry.ExtremeSmithing(recipeId, target,
                        imported.ingredients.get(0), imported.ingredients.get(1), imported.ingredients.get(2),
                        itemStack(smithing.getResultItem(registryAccess))));
            }
            return failure(recipeId, "kind", "recipe.kind.unsupported",
                    "Only Avaritia 1.4.1 visual-editor recipe types are supported");
        } catch (RuntimeException exception) {
            return failure(recipeId, "$", "recipe.import.failed",
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private RecipeImportResult importShaped(ResourceLocation recipeId, ShapedTableCraftingRecipe recipe,
                                            OutputTarget target, RegistryAccess registryAccess) {
        Optional<CraftingTier> tier = CraftingTier.fromValue(recipe.getTier());
        if (tier.isEmpty()) {
            return failure(recipeId, "tier", "recipe.tier.unsupported", "Unsupported table tier");
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
                if (imported instanceof IngredientImportResult.Failure importFailure) {
                    return failure(recipeId, importFailure);
                }
                grid.put(row * tier.get().gridSize() + column,
                        ((IngredientImportResult.Success) imported).ingredient());
            }
        }
        return new RecipeImportResult.Success(new CustomizationEntry.ShapedTable(recipeId, target,
                tier.get(), grid, itemStack(recipe.getResultItem(registryAccess))));
    }

    private RecipeImportResult importShapeless(ResourceLocation recipeId, ShapelessTableCraftingRecipe recipe,
                                               OutputTarget target, RegistryAccess registryAccess) {
        Optional<CraftingTier> tier = CraftingTier.fromValue(recipe.getTier());
        if (tier.isEmpty()) {
            return failure(recipeId, "tier", "recipe.tier.unsupported", "Unsupported table tier");
        }
        ImportedIngredients imported = importIngredients(recipeId, recipe.getIngredients(), null);
        if (imported.failure != null) {
            return imported.failure;
        }
        return new RecipeImportResult.Success(new CustomizationEntry.ShapelessTable(recipeId, target,
                tier.get(), imported.ingredients, itemStack(recipe.getResultItem(registryAccess))));
    }

    private RecipeImportResult importCatalyst(ResourceLocation recipeId, InfinityCatalystCraftRecipe recipe,
                                              OutputTarget target, RegistryAccess registryAccess) {
        DecodedSpecial decoded = this.specialRecipes.catalyst(recipe, registryAccess);
        ImportedIngredients imported = importIngredients(recipeId, decoded.ingredients, null);
        if (imported.failure != null) {
            return imported.failure;
        }
        return new RecipeImportResult.Success(new CustomizationEntry.InfinityCatalyst(recipeId, target,
                decoded.group, imported.ingredients, decoded.count));
    }

    private RecipeImportResult importEternal(ResourceLocation recipeId, EternalSingularityCraftRecipe recipe,
                                             OutputTarget target, RegistryAccess registryAccess) {
        DecodedSpecial decoded = this.specialRecipes.eternal(recipe, registryAccess);
        ImportedIngredients imported = importIngredients(recipeId, decoded.ingredients, null);
        if (imported.failure != null) {
            return imported.failure;
        }
        return new RecipeImportResult.Success(new CustomizationEntry.EternalSingularity(recipeId, target,
                imported.ingredients, decoded.count));
    }

    private ImportedIngredients importIngredients(ResourceLocation recipeId, List<Ingredient> encoded,
                                                  List<String> fieldNames) {
        List<IngredientSpec> result = new ArrayList<>();
        for (int index = 0; index < encoded.size(); index++) {
            String field = fieldNames == null ? "ingredients[" + index + "]" : fieldNames.get(index);
            IngredientImportResult imported = this.ingredientImporter.importIngredient(encoded.get(index), field);
            if (imported instanceof IngredientImportResult.Failure importFailure) {
                return new ImportedIngredients(List.of(), failure(recipeId, importFailure));
            }
            result.add(((IngredientImportResult.Success) imported).ingredient());
        }
        return new ImportedIngredients(List.copyOf(result), null);
    }

    private ItemStackSpec itemStack(ItemStack stack) {
        return this.itemStacks.read(stack);
    }

    private static DecodedSpecial decodeCatalystNetwork(InfinityCatalystCraftRecipe recipe,
                                                        RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registryAccess, ConnectionType.OTHER);
        try {
            InfinityCatalystCraftRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe);
            String group = buffer.readUtf();
            int size = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            return new DecodedSpecial(group, ingredients, buffer.readInt());
        } finally {
            buffer.release();
        }
    }

    private static DecodedSpecial decodeEternalNetwork(EternalSingularityCraftRecipe recipe,
                                                       RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registryAccess, ConnectionType.OTHER);
        try {
            EternalSingularityCraftRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe);
            int size = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            return new DecodedSpecial("", ingredients, buffer.readInt());
        } finally {
            buffer.release();
        }
    }

    private static RecipeImportResult.Failure failure(ResourceLocation recipeId,
                                                       IngredientImportResult.Failure failure) {
        return failure(recipeId, failure.fieldPath(), failure.code(), failure.message());
    }

    private static RecipeImportResult.Failure failure(ResourceLocation recipeId, String field,
                                                       String code, String message) {
        return new RecipeImportResult.Failure(recipeId, field, code, message);
    }

    @FunctionalInterface
    interface ItemStackReader {
        ItemStackSpec read(ItemStack stack);
    }

    interface SpecialRecipeReader {
        DecodedSpecial catalyst(InfinityCatalystCraftRecipe recipe, RegistryAccess registryAccess);

        DecodedSpecial eternal(EternalSingularityCraftRecipe recipe, RegistryAccess registryAccess);
    }

    record DecodedSpecial(String group, List<Ingredient> ingredients, int count) {
        DecodedSpecial {
            ingredients = List.copyOf(ingredients);
        }
    }

    private static final class NetworkSpecialRecipeReader implements SpecialRecipeReader {
        @Override
        public DecodedSpecial catalyst(InfinityCatalystCraftRecipe recipe, RegistryAccess registryAccess) {
            return decodeCatalystNetwork(recipe, registryAccess);
        }

        @Override
        public DecodedSpecial eternal(EternalSingularityCraftRecipe recipe, RegistryAccess registryAccess) {
            return decodeEternalNetwork(recipe, registryAccess);
        }
    }

    private record ImportedIngredients(List<IngredientSpec> ingredients,
                                       RecipeImportResult.Failure failure) {
    }
}
