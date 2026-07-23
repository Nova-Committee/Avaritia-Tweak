package committee.nova.mods.avaritia_tweak.customization.model;

import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public sealed interface IngredientSpec permits IngredientSpec.Item, IngredientSpec.Components, IngredientSpec.Tag,
        IngredientSpec.Choice {
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

    /** A NeoForge data-component predicate retained verbatim for lossless recipe imports. */
    record Components(ResourceLocation itemId, String componentsJson, boolean strict) implements IngredientSpec {
        public Components {
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(componentsJson, "componentsJson");
            var parsed = JsonParser.parseString(componentsJson);
            if (!parsed.isJsonObject() || parsed.getAsJsonObject().size() == 0) {
                throw new IllegalArgumentException("Component ingredients require a non-empty JSON object");
            }
            componentsJson = parsed.toString();
        }
    }

    /** A lossless logical OR over two or more item ingredients. */
    record Choice(List<IngredientSpec> alternatives) implements IngredientSpec {
        public Choice {
            Objects.requireNonNull(alternatives, "alternatives");
            List<IngredientSpec> flattened = new ArrayList<>();
            for (IngredientSpec alternative : alternatives) {
                Objects.requireNonNull(alternative, "alternative");
                if (alternative instanceof Choice nested) {
                    flattened.addAll(nested.alternatives());
                } else {
                    flattened.add(alternative);
                }
            }
            if (flattened.size() < 2) {
                throw new IllegalArgumentException("Choice ingredients require at least two alternatives");
            }
            alternatives = List.copyOf(flattened);
        }
    }
}
