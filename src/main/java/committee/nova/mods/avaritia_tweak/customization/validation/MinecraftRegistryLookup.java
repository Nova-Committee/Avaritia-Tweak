package committee.nova.mods.avaritia_tweak.customization.validation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.OptionalInt;

public final class MinecraftRegistryLookup implements RegistryLookup {
    @Override
    public OptionalInt itemMaxStackSize(ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        return item == null
                ? OptionalInt.empty()
                : OptionalInt.of(item.getDefaultInstance().getMaxStackSize());
    }

    @Override
    public boolean itemTagExists(ResourceLocation tagId) {
        ITagManager<Item> tags = ForgeRegistries.ITEMS.tags();
        return tags != null && tags.getTagNames().anyMatch(tag -> tag.location().equals(tagId));
    }
}
