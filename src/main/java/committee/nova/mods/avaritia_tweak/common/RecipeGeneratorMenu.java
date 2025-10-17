package committee.nova.mods.avaritia_tweak.common;

import committee.nova.mods.avaritia.api.common.menu.BaseTileMenu;
import committee.nova.mods.avaritia_tweak.client.script.OutType;
import committee.nova.mods.avaritia_tweak.client.script.RecipeTypes;
import committee.nova.mods.avaritia_tweak.init.ModReg;
import committee.nova.mods.avaritia_tweak.util.CrtUtils;
import committee.nova.mods.avaritia_tweak.util.KubeJsUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author: cnlimiter
 */
public class RecipeGeneratorMenu extends BaseTileMenu<RecipeGeneratorTile> {
    @Getter private RecipeTypes category = RecipeTypes.AVARITIA_EXTREME_CRAFTING;
    @Getter private List<Integer> availableSlots = new ArrayList<>();
    @Getter private final Map<Integer, Slot> slotMap = new HashMap<>();

    public RecipeGeneratorMenu(int id, Inventory playerInventory, @NotNull BlockPos blockPos) {
        super(ModReg.recipe_generator_menu.get(), id, playerInventory, blockPos);
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                this.addCraftingSlot(slotIndex, x, y);
            }
        }
        this.addCraftingSlot(81, 202, 18 + 4 * 18);
        createInventorySlots(playerInventory, 31, 110);
    }

    public static RecipeGeneratorMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        return new RecipeGeneratorMenu(containerId, inventory, buf.readBlockPos());
    }

    public void switchCategory(RecipeTypes newCategory) {
        if (this.category != newCategory) {
            this.category = newCategory;

            // 清除现有槽位
            clearAll();
            // 重新设置槽位
            setupSlotsForCategory(newCategory);

            this.slots.forEach(slot -> {
                System.out.println("slot.index: " + slot.index);
            });

        }
    }

    public void clearAll() {
            // 清除现有槽位
            this.slots.clear();
            this.lastSlots.clear();
            this.remoteSlots.clear();
            this.availableSlots.clear();
            this.slotMap.clear();
    }


    private void setupSlotsForCategory(RecipeTypes category) {

        switch (category) {
            case VANILLA_CRAFTING, AVARITIA_SCULK_CRAFTING ->  setup3_3CraftingSlots();
            case VANILLA_SMITHING ->  setupVanillaSmithingSlots();
            case VANILLA_FURNACE, VANILLA_STONECUTTING ->  setupVanillaFurnaceSlots();
            case AVARITIA_NETHER_CRAFTING ->  setup5_5CraftingSlots();
            case AVARITIA_END_CRAFTING ->  setup7_7CraftingSlots();
            case AVARITIA_EXTREME_CRAFTING ->  setup9_9CraftingSlots();
            case AVARITIA_EXTREME_SMITHING ->  setupAvaritiaSmithingSlots();
            case AVARITIA_COMPRESSOR ->  setupAvaritiaCompressorSlots();
        }

        // 添加输出槽位 (固定位置)
        this.addCraftingSlot(81, 202, 18 + 4 * 18);
    }

    // 改进槽位添加方法
    private void addCraftingSlot(int slotIndex, int x, int y) {
        Slot slot = new Slot(getTileEntity().containers, slotIndex, x, y);
        this.addSlot(slot);
        slotMap.put(slotIndex, slot);
        availableSlots.add(slotIndex);
    }

    // 提供安全的槽位获取方法
    public Slot getSafeSlot(int slotIndex) {
        return slotMap.get(slotIndex);
    }


    // 获取指定槽位的物品
    public ItemStack getSlotItem(int slotIndex) {
        return getSafeSlot(slotIndex).getItem();
    }

    // 改进槽位检查方法
    public boolean isSlotValid(int slotIndex) {
        return slotMap.containsKey(slotIndex) && availableSlots.contains(slotIndex);
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
                this.addCraftingSlot(slotIndex, x, y);
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
                this.addCraftingSlot(slotIndex, x, y);
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
                this.addCraftingSlot(slotIndex, x, y);
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
                this.addCraftingSlot(slotIndex, x, y);
            }
        }
    }

    private void setupVanillaSmithingSlots() {
        // 锻造台槽位 (39, 40, 41)
        this.addCraftingSlot(39, 8 + 3 * 18, 18 + 4 * 18);
        this.addCraftingSlot(40, 8 + 4 * 18, 18 + 4 * 18);
        this.addCraftingSlot(41, 8 + 5 * 18, 18 + 4 * 18);
    }

    private void setupAvaritiaSmithingSlots() {
        // 锻造台槽位 (31, 39, 40, 41, 49)
        this.addCraftingSlot(31, 8 + 4 * 18, 18 + 3 * 18);
        this.addCraftingSlot(39, 8 + 3 * 18, 18 + 4 * 18);
        this.addCraftingSlot(40, 8 + 4 * 18, 18 + 4 * 18);
        this.addCraftingSlot(41, 8 + 5 * 18, 18 + 4 * 18);
        this.addCraftingSlot(49, 8 + 4 * 18, 18 + 5 * 18);
    }

    private void setupAvaritiaCompressorSlots() {
        // 压缩机槽位 (40)
        this.addCraftingSlot(40, 8 + 4 * 18, 18 + 4 * 18);
    }

    private void setupVanillaFurnaceSlots() {
        // 熔炉槽位 (40)
        this.addCraftingSlot(40, 8 + 4 * 18, 18 + 4 * 18);
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

    private void doGenerateScript(String fileName, OutType scriptType) {
        switch (scriptType) {
            case JS: {
                KubeJsUtils.exportTableJS(this, true, 4, true, fileName);
            }
            case ZS: {
                CrtUtils.exportTableZS(this, true, 4, true, fileName);
            }
        }
    }
}
