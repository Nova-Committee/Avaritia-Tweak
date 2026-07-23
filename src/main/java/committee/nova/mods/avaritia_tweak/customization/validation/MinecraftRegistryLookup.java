package committee.nova.mods.avaritia_tweak.customization.validation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.OptionalInt;

public final class MinecraftRegistryLookup implements RegistryLookup {
    @Override
    public OptionalInt itemMaxStackSize(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(item -> OptionalInt.of(item.getDefaultInstance().getMaxStackSize()))
                .orElseGet(OptionalInt::empty);
    }

    @Override
    public boolean itemTagExists(ResourceLocation tagId) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        return BuiltInRegistries.ITEM.getTag(tag).isPresent();
    }
}
