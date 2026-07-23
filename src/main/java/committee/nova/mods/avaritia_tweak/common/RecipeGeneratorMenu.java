package committee.nova.mods.avaritia_tweak.common;

import committee.nova.mods.avaritia_tweak.init.ModReg;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal block menu used only to open the client-local visual editor.
 *
 * <p>The editor intentionally owns no {@code Slot}: ghost ingredients are client widgets and never
 * transfer items, mutate the player inventory, or touch the legacy block-entity container.</p>
 */
public final class RecipeGeneratorMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;

    public RecipeGeneratorMenu(int id, Inventory playerInventory, @NotNull BlockPos blockPos) {
        super(ModReg.recipe_generator_menu.get(), id);
        this.blockPos = blockPos.immutable();
    }

    public static RecipeGeneratorMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        return new RecipeGeneratorMenu(containerId, inventory, buffer.readBlockPos());
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.distanceToSqr(this.blockPos.getX() + 0.5,
                this.blockPos.getY() + 0.5, this.blockPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }
}
