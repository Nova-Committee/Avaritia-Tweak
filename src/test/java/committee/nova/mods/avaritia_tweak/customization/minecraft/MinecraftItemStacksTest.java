package committee.nova.mods.avaritia_tweak.customization.minecraft;

import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinecraftItemStacksTest {
    @Test
    void roundTripsLegacyStrictNbtThroughCustomDataComponent() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("mode", "strict");
        ItemStackSpec expected = new ItemStackSpec(id("minecraft:stone"), 3, Optional.of(nbt));

        ItemStack stack = MinecraftItemStacks.fromSpec(expected);
        ItemStackSpec restored = MinecraftItemStacks.toSpec(stack);

        assertThat(restored).isEqualTo(expected);
        assertThat(restored.nbt().orElseThrow()).isNotSameAs(nbt);
    }

    @Test
    void returnsEmptyStackForUnknownRegistryId() {
        ItemStack stack = MinecraftItemStacks.fromSpec(
                new ItemStackSpec(id("missing:unknown_item"), 1));

        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    void rejectsComponentsThatTheWorkspaceModelCannotPreserve() {
        ItemStack stack = MinecraftItemStacks.fromSpec(new ItemStackSpec(id("minecraft:stone"), 1));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        assertThatThrownBy(() -> MinecraftItemStacks.toSpec(stack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minecraft:custom_name");
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
