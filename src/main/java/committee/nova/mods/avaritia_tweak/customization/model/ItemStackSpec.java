package committee.nova.mods.avaritia_tweak.customization.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

public record ItemStackSpec(ResourceLocation itemId, int count, Optional<CompoundTag> nbt) {
    public ItemStackSpec {
        Objects.requireNonNull(itemId, "itemId");
        nbt = Objects.requireNonNull(nbt, "nbt").map(CompoundTag::copy);
    }

    public ItemStackSpec(ResourceLocation itemId, int count) {
        this(itemId, count, Optional.empty());
    }

    @Override
    public Optional<CompoundTag> nbt() {
        return this.nbt.map(CompoundTag::copy);
    }
}
