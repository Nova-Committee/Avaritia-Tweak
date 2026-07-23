package committee.nova.mods.avaritia_tweak.customization.validation;

import net.minecraft.resources.ResourceLocation;

import java.util.OptionalInt;

/** Registry access boundary used by validation and replaceable by unit tests. */
public interface RegistryLookup {
    OptionalInt itemMaxStackSize(ResourceLocation itemId);

    boolean itemTagExists(ResourceLocation tagId);
}
