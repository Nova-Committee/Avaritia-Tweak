package committee.nova.mods.avaritia_tweak.customization.minecraft;

import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public final class MinecraftItemStacks {
    private MinecraftItemStacks() {
    }

    public static Optional<Item> findItem(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.getOptional(itemId);
    }

    public static ItemStack fromSpec(ItemStackSpec spec) {
        return findItem(spec.itemId()).map(item -> {
            ItemStack stack = new ItemStack(item, spec.count());
            spec.nbt().ifPresent(tag -> stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy())));
            return stack;
        }).orElse(ItemStack.EMPTY);
    }

    public static ItemStackSpec toSpec(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Recipe output is empty");
        }
        stack.getComponentsPatch().entrySet().stream()
                .filter(entry -> entry.getKey() != DataComponents.CUSTOM_DATA)
                .findFirst()
                .ifPresent(entry -> {
                    ResourceLocation componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
                    throw new IllegalArgumentException(
                            "Recipe output uses unsupported data component " + componentId);
                });
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            throw new IllegalArgumentException("Recipe output item is not registered");
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        Optional<CompoundTag> nbt = customData == null || customData.isEmpty()
                ? Optional.empty()
                : Optional.of(customData.copyTag());
        return new ItemStackSpec(itemId, stack.getCount(), nbt);
    }
}
