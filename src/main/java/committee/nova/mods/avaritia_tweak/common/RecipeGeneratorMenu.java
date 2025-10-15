package committee.nova.mods.avaritia_tweak.common;

import committee.nova.mods.avaritia.api.common.menu.BaseTileMenu;
import committee.nova.mods.avaritia_tweak.client.script.RecipeTypes;
import committee.nova.mods.avaritia_tweak.init.ModReg;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: cnlimiter
 */
public class RecipeGeneratorMenu extends BaseTileMenu<RecipeGeneratorTile> {
    @Getter private RecipeTypes category = RecipeTypes.VANILLA_CRAFTING;
    @Getter private List<Integer> availableSlots = new ArrayList<>();

    public RecipeGeneratorMenu(int id, Inventory playerInventory, @NotNull BlockPos blockPos) {
        super(ModReg.recipe_generator_menu.get(), id, playerInventory, blockPos);

        // 添加输出槽位 (81)，放在GUI右侧
        this.addSlot(new Slot(getTileEntity().containers, 81, 202, 89));
    }

    public static RecipeGeneratorMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        return new RecipeGeneratorMenu(containerId, inventory, buf.readBlockPos());
    }

    public void switchCategory(RecipeTypes newCategory) {
        if (this.category != newCategory) {
            this.category = newCategory;

            // 清除现有槽位
            this.slots.clear();
            this.lastSlots.clear();
            this.remoteSlots.clear();

            // 重新设置槽位
            setupSlotsForCategory(newCategory);
        }
    }

    private void setupSlotsForCategory(RecipeTypes category) {
        availableSlots.clear();

        switch (category) {
            case VANILLA_CRAFTING ->  setup3_3CraftingSlots();
            case VANILLA_SMITHING ->  setupVanillaSmithingSlots();
            case VANILLA_FURNACE ->  setupVanillaFurnaceSlots();
            case VANILLA_STONECUTTING ->  setupVanillaFurnaceSlots();
            case AVARITIA_SCULK_CRAFTING ->  setup3_3CraftingSlots();
            case AVARITIA_NETHER_CRAFTING ->  setup5_5CraftingSlots();
            case AVARITIA_END_CRAFTING ->  setup7_7CraftingSlots();
            case AVARITIA_EXTREME_CRAFTING ->  setup9_9CraftingSlots();
            case AVARITIA_EXTREME_SMITHING ->  setupAvaritiaSmithingSlots();
            case AVARITIA_COMPRESSOR ->  setupAvaritiaCompressorSlots();
        }

        // 添加输出槽位 (固定位置)
        this.addSlot(new Slot(getTileEntity().containers, 81, 202, 18 + 4 * 18));
        availableSlots.add(81);
    }

    private void setup3_3CraftingSlots() {
        // 中心3x3区域 (槽位30-56)
        int startRow = 3;
        int startCol = 3;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = (startRow + row) * 9 + (startCol + col);
                int x = 8 + 3 * 18  + col * 18;
                int y = 18 + 3 * 18  + row * 18;
                this.addSlot(new Slot(getTileEntity().containers, slotIndex, x, y));
                availableSlots.add(slotIndex);
            }
        }
    }

    private void setup5_5CraftingSlots() {
        // 中心5x5区域 (槽位20-60)
        int startRow = 2;
        int startCol = 2;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = (startRow + row) * 9 + (startCol + col);
                int x = 8 + 2 * 18  + col * 18;
                int y = 18 + 2 * 18  + row * 18;
                this.addSlot(new Slot(getTileEntity().containers, slotIndex, x, y));
                availableSlots.add(slotIndex);
            }
        }
    }

    private void setup7_7CraftingSlots() {
        // 中心7x7区域 (槽位10-70)
        int startRow = 1;
        int startCol = 1;

        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                int slotIndex = (startRow + row) * 9 + (startCol + col);
                int x =  8 + 18 + col * 18;
                int y = 18 + 18 + row * 18;
                this.addSlot(new Slot(getTileEntity().containers, slotIndex, x, y));
                availableSlots.add(slotIndex);
            }
        }
    }

    private void setup9_9CraftingSlots() {
        // 完整9x9区域 (槽位0-80)
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                this.addSlot(new Slot(getTileEntity().containers, slotIndex, x, y));
                availableSlots.add(slotIndex);
            }
        }
    }

    private void setupVanillaSmithingSlots() {
        // 锻造台槽位 (39, 40, 41)
        this.addSlot(new Slot(getTileEntity().containers, 39, 8 + 3 * 18, 18 + 4 * 18)); // 模板
        this.addSlot(new Slot(getTileEntity().containers, 40, 8 + 4 * 18, 18 + 4 * 18)); // 基础物品
        this.addSlot(new Slot(getTileEntity().containers, 41, 8 + 5 * 18, 18 + 4 * 18)); // 添加物品
        availableSlots.add(39);
        availableSlots.add(40);
        availableSlots.add(41);
    }

    private void setupAvaritiaSmithingSlots() {
        // 锻造台槽位 (31, 39, 40, 41, 49)
        this.addSlot(new Slot(getTileEntity().containers, 31, 8 + 4 * 18, 18 + 3 * 18)); // 添加物品1
        this.addSlot(new Slot(getTileEntity().containers, 39, 8 + 3 * 18, 18 + 4 * 18)); // 模板
        this.addSlot(new Slot(getTileEntity().containers, 40, 8 + 4 * 18, 18 + 4 * 18)); // 基础物品
        this.addSlot(new Slot(getTileEntity().containers, 41, 8 + 5 * 18, 18 + 4 * 18)); // 添加物品2
        this.addSlot(new Slot(getTileEntity().containers, 49, 8 + 4 * 18, 18 + 5 * 18)); // 添加物品3
        availableSlots.add(31);
        availableSlots.add(39);
        availableSlots.add(40);
        availableSlots.add(41);
        availableSlots.add(49);
    }

    private void setupAvaritiaCompressorSlots() {
        // 压缩机槽位 (40)
        this.addSlot(new Slot(getTileEntity().containers, 40, 8 + 4 * 18, 18 + 4 * 18)); // 基础物品
        availableSlots.add(40);
    }

    private void setupVanillaFurnaceSlots() {
        // 熔炉槽位 (40)
        this.addSlot(new Slot(getTileEntity().containers, 40, 8 + 4 * 18, 18 + 4 * 18));
        availableSlots.add(40);
    }

    // 获取指定槽位的物品
    public ItemStack getSlotItem(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < this.slots.size()) {
            return this.slots.get(slotIndex).getItem();
        }
        return ItemStack.EMPTY;
    }


    // 根据等级获取可用槽位数量
    public int getAvailableSlotsForTier(int tier) {
        return switch (tier) {
            case 1 -> 9;   // 3x3
            case 2 -> 25;  // 5x5
            case 3 -> 49;  // 7x7
            case 4 -> 81;  // 9x9
            default -> 81;
        };
    }

    // 获取指定等级的可用槽位集合（从中心向外扩散）
    public Set<Integer> getAvailableSlotsSetForTier(int tier) {
        Set<Integer> availableSlots = new HashSet<>();

        // 根据等级确定网格大小
        int gridSize = switch (tier) {
            case 1 -> 3;   // 3x3
            case 2 -> 5;   // 5x5
            case 3 -> 7;   // 7x7
            case 4 -> 9;   // 9x9
            default -> 9;
        };

        // 计算起始位置（从中心向外扩散）
        int startRow = (9 - gridSize) / 2;
        int startCol = (9 - gridSize) / 2;

        // 添加可用槽位
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int slotIndex = (startRow + row) * 9 + (startCol + col);
                if (slotIndex < 81) {
                    availableSlots.add(slotIndex);
                }
            }
        }

        return availableSlots;
    }



}
