package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** Consistent name-first, resource-ID-second projection for every displayed item. */
record ItemDisplayText(String name, String id) {
    ItemDisplayText {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(id, "id");
    }

    static ItemDisplayText of(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (!isBootstrapped()) {
            return new ItemDisplayText(id.toString(), id.toString());
        }
        String name = BuiltInRegistries.ITEM.getOptional(id)
                .map(item -> new ItemStack(item).getHoverName().getString())
                .orElse(id.toString());
        return new ItemDisplayText(name, id.toString());
    }

    static ItemDisplayText of(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (!isBootstrapped()) {
            return new ItemDisplayText(stack.getHoverName().getString(), "minecraft:air");
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return new ItemDisplayText(stack.getHoverName().getString(), id.toString());
    }

    String inline() {
        return this.name.equals(this.id) ? this.id : this.name + " · " + this.id;
    }

    List<Component> tooltip(String nameSuffix, String idSuffix) {
        if (this.name.equals(this.id)) {
            return List.of(Component.literal(this.id + nameSuffix + idSuffix));
        }
        return List.of(Component.literal(this.name + nameSuffix),
                Component.literal(this.id + idSuffix).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean isBootstrapped() {
        try {
            Bootstrap.checkBootstrapCalled(() -> "Item display registry lookup");
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
