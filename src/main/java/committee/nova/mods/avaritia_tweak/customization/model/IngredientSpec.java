package committee.nova.mods.avaritia_tweak.customization.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

public sealed interface IngredientSpec permits IngredientSpec.Item, IngredientSpec.Tag {
    record Item(ResourceLocation itemId, Optional<CompoundTag> strictNbt) implements IngredientSpec {
        public Item {
            Objects.requireNonNull(itemId, "itemId");
            strictNbt = Objects.requireNonNull(strictNbt, "strictNbt").map(CompoundTag::copy);
        }

        public Item(ResourceLocation itemId) {
            this(itemId, Optional.empty());
        }

        @Override
        public Optional<CompoundTag> strictNbt() {
            return this.strictNbt.map(CompoundTag::copy);
        }
    }

    record Tag(ResourceLocation tagId) implements IngredientSpec {
        public Tag {
            Objects.requireNonNull(tagId, "tagId");
        }
    }
}
