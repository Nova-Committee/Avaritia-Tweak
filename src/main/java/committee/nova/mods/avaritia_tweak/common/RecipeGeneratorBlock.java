package committee.nova.mods.avaritia_tweak.common;

import committee.nova.mods.avaritia.api.common.block.BaseTileEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: cnlimiter
 */
public class RecipeGeneratorBlock extends BaseTileEntityBlock {
    public RecipeGeneratorBlock() {
        super(MapColor.METAL, SoundType.METAL, 50f, 2000f, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new RecipeGeneratorTile(blockPos, blockState);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level,
                                                        @NotNull BlockPos pos, @NotNull Player player,
                                                        @NotNull BlockHitResult trace) {
        if (!level.isClientSide() && !player.isSpectator()) {
            var tile = level.getBlockEntity(pos);

            if (player instanceof ServerPlayer serverPlayer && tile instanceof RecipeGeneratorTile generatorTile) {
                serverPlayer.openMenu(generatorTile, pos);
            }
        }

        return InteractionResult.SUCCESS;
    }

}
