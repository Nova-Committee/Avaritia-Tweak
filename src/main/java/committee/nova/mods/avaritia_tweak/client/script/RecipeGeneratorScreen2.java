package committee.nova.mods.avaritia_tweak.client.script;

import committee.nova.mods.avaritia.api.client.screen.BaseContainerScreen;
import committee.nova.mods.avaritia.api.client.screen.ItemSelectScreen;
import committee.nova.mods.avaritia.api.client.screen.StringInputScreen;
import committee.nova.mods.avaritia.api.client.screen.component.CyclingTextureButton;
import committee.nova.mods.avaritia.api.client.screen.component.KeyEventManager;
import committee.nova.mods.avaritia.api.client.screen.component.OperationButton;
import committee.nova.mods.avaritia.api.client.screen.component.Text;
import committee.nova.mods.avaritia.api.client.screen.coordinate.Coordinate;
import committee.nova.mods.avaritia.api.client.util.GuiUtils;
import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia_tweak.AvaritiaTweak;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorMenu;
import committee.nova.mods.avaritia_tweak.util.CrtUtils;
import committee.nova.mods.avaritia_tweak.util.KubeJsUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author cnlimiter
 */
public class RecipeGeneratorScreen2 extends BaseContainerScreen<RecipeGeneratorMenu> {
    public static final ResourceLocation BACKGROUND = new ResourceLocation(AvaritiaTweak.MOD_ID, "textures/gui/recipe_generator_back.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation(AvaritiaTweak.MOD_ID, "textures/gui/recipe_generator_craft.png");

    private final KeyEventManager keyManager = new KeyEventManager();

    // 当前选中的选项卡
    private RecipeTypes currentTab = RecipeTypes.VANILLA_CRAFTING;
    // 模式控制
    private boolean selectMode = true; // 默认为选择模式
    private ItemStack brushItem = ItemStack.EMPTY;
    // 脚本生成设置
    private int scriptType = 1; // 1=KubeJS, 2=CRT

    // UI组件
    private OperationButton modeToggleButton;
    private OperationButton brushButton;
    private OperationButton recipeFillButton; // 新增配方填充按钮
    private OperationButton scriptTypeButton;
    private OperationButton generateButton;

    // 选项卡按钮映射
    private final Map<RecipeTypes, OperationButton> tabButtons = new HashMap<>();
    private final List<OperationButton> actionButtons = new ArrayList<>();

    public RecipeGeneratorScreen2(RecipeGeneratorMenu container, Inventory inventory, Component title) {
        super(container, inventory, title, BACKGROUND, 234, 278, 512, 512);
    }

    @Override
    protected void subInit() {
        super.subInit();
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;

        // 初始化选项卡按钮
        initTabButtons(centerX, centerY);

        // 初始化其他控件
        initControlButtons(centerX, centerY);
    }

    private void initTabButtons(int centerX, int centerY) {
        // 清除旧按钮
        tabButtons.clear();

        // 创建新的选项卡按钮
        int tabIndex = 0;
        for (RecipeTypes tab : RecipeTypes.values()) {
            OperationButton tabButton = new OperationButton(tab.ordinal(), BACKGROUND)
                    .setX(centerX).setY(centerY + tabIndex * 23)
                    .setNormal(tab.normal)
                    .setTap(tab.tap);
            tabButtons.put(tab, tabButton);
            tabIndex++;
        }
    }

    private void initControlButtons(int centerX, int centerY) {
        // 模式切换按钮
        modeToggleButton = new OperationButton(100, BACKGROUND)
                .setX(centerX + 203).setY(centerY + 3)
                .setNormal(new Coordinate().setU0(235).setV0(140).setUWidth(22).setVHeight(24))
                .setTap(new Coordinate().setU0(258).setV0(140).setUWidth(22).setVHeight(24));
        actionButtons.add(modeToggleButton);

        // 画笔物品/配方选择按钮
        brushButton = new OperationButton(101, BACKGROUND)
                .setX(centerX + 203).setY(centerY + 26)
                .setNormal(new Coordinate().setU0(235).setV0(115).setUWidth(22).setVHeight(24))
                .setTap(new Coordinate().setU0(258).setV0(115).setUWidth(22).setVHeight(24));
        actionButtons.add(brushButton);

        // 脚本类型选择按钮
        scriptTypeButton = new CyclingTextureButton(102, BACKGROUND)
                .setTextureCoordinates(List.of(
                        new Coordinate().setU0(235).setV0(0).setUWidth(22).setVHeight(24)
                        , new Coordinate().setU0(235).setV0(25).setUWidth(22).setVHeight(24)
                        , new Coordinate().setU0(235).setV0(50).setUWidth(22).setVHeight(24)
                ), null, null)
                .setX(centerX + 203).setY(centerY + 155);
        actionButtons.add(scriptTypeButton);

        // 生成按钮
        generateButton = new OperationButton(103, BACKGROUND)
                .setX(centerX + 226).setY(centerY + 155)
                .setNormal(new Coordinate().setU0(235).setV0(190).setUWidth(22).setVHeight(24))
                .setTap(new Coordinate().setU0(258).setV0(190).setUWidth(22).setVHeight(24));
        actionButtons.add(generateButton);
    }

    private void switchToTab(RecipeTypes tab) {
        if (this.currentTab != tab) {
            this.currentTab = tab;
            // 通知菜单切换类别
            this.menu.switchCategory(tab);
        }
    }

    private void toggleMode() {
        this.selectMode = !this.selectMode;
    }

    private void openBrushItemSelector() {
        this.minecraft.setScreen(new ItemSelectScreen(
                this,
                (itemStack) -> this.brushItem = itemStack.copy(),
                brushItem.isEmpty() ? new ItemStack(Items.AIR) : brushItem
        ));
    }

    private void renderButton(GuiGraphics graphics) {
        for (OperationButton button : tabButtons.values()) button.render(graphics, keyManager);
        for (OperationButton button : actionButtons) button.render(graphics, keyManager);
        for (OperationButton button : tabButtons.values()) button.renderPopup(graphics, this.font, keyManager);
        for (OperationButton button : actionButtons) button.renderPopup(graphics, this.font, keyManager);
    }

    private void renderSlots(GuiGraphics graphics) {
        switch (currentTab) {
            case AVARITIA_EXTREME_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, 3, 3, 0, 0, 169, 179, 512, 512);
            }
            case AVARITIA_END_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, 20, 28, 0, 180, 132, 133, 512, 512);
            }
            case AVARITIA_NETHER_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, 37, 50, 0, 314, 96, 97, 512, 512);
            }
            case AVARITIA_SCULK_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, 54, 65, 0, 412, 60, 61, 512, 512);
            }
            case AVARITIA_EXTREME_SMITHING -> {
                GuiUtils.blit(graphics, WIDGETS, 32, 62, 172, 14, 60, 62, 512, 512);
            }
            case AVARITIA_COMPRESSOR -> {
                GuiUtils.blit(graphics, WIDGETS, 32, 75, 172, 104, 46, 26, 512, 512);
            }
            case VANILLA_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, 54, 65, 0, 412, 60, 61, 512, 512);
            }
            case VANILLA_SMITHING -> {
                GuiUtils.blit(graphics, WIDGETS, 32, 75, 172, 77, 60, 26, 512, 512);
            }
            case VANILLA_FURNACE -> {
                GuiUtils.blit(graphics, WIDGETS, 49, 75, 266, 14, 24, 26, 512, 512);
            }
            case VANILLA_STONECUTTING -> {
                GuiUtils.blit(graphics, WIDGETS, 49, 75, 266, 14, 24, 26, 512, 512);
            }
        }
        GuiUtils.blit(graphics, WIDGETS, 190, 72, 233, 14, 32, 34, 512, 512);

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderBgs(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBgs(guiGraphics, partialTick, mouseX, mouseY);
        this.renderButton(guiGraphics);
        this.renderSlots(guiGraphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        keyManager.mouseScrolled(delta, mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        keyManager.mouseClicked(button, mouseX, mouseY);
        if (button == 0) { // 左键点击
            // 检查输入槽位区域 (9x9网格)
            int gridX = (int) ((mouseX - (this.leftPos + 8)) / 18);
            int gridY = (int) ((mouseY - (this.topPos + 18)) / 18);

            if (gridX >= 0 && gridX < 9 && gridY >= 0 && gridY < 9) {
                // 点击了输入槽位
                int slotIndex = gridY * 9 + gridX;
                if (isSlotAvailable(slotIndex)) {
                    if (selectMode) {
                        // 选择模式：打开物品选择界面
                        openItemSelectScreen(slotIndex);
                    } else {
                        // 画刷模式：用选定的物品填充槽位
                        if (!brushItem.isEmpty()) {
                            this.menu.getSlot(slotIndex).set(brushItem.copy());
                        }
                    }
                    return true;
                }
            }

            // 检查输出槽位
            int outputX = (int) ((mouseX - (this.leftPos + 202)) / 18);
            int outputY = (int) ((mouseY - (this.topPos + 89)) / 18);

            if (outputX == 0 && outputY == 0) {
                // 点击了输出槽位
                openItemSelectScreen(81);
                return true;
            }
        } else if (button == 1) { // 右键点击清除
            // 检查输入槽位区域 (9x9网格)
            int gridX = (int) ((mouseX - (this.leftPos + 8)) / 18);
            int gridY = (int) ((mouseY - (this.topPos + 18)) / 18);

            if (gridX >= 0 && gridX < 9 && gridY >= 0 && gridY < 9) {
                // 点击了输入槽位
                int slotIndex = gridY * 9 + gridX;
                if (isSlotAvailable(slotIndex)) {
                    if (!this.menu.getSlotItem(slotIndex).isEmpty()) {
                        this.menu.getSlot(slotIndex).set(ItemStack.EMPTY);
                    }
                    return true;
                }
            }

            // 检查输出槽位
            int outputX = (int) ((mouseX - (this.leftPos + 202)) / 18);
            int outputY = (int) ((mouseY - (this.topPos + 89)) / 18);

            if (outputX == 0 && outputY == 0) {
                if (!this.menu.getSlotItem(81).isEmpty()) {
                    this.menu.getSlot(81).set(ItemStack.EMPTY);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        keyManager.refresh(mouseX, mouseY);
        AtomicBoolean flag = new AtomicBoolean(false);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // 选项卡按钮
            tabButtons.forEach((key, value) -> {
                if (value.isHovered() && value.isPressed()) {
                    switchToTab(RecipeTypes.valueOf(value.getOperation()));
                    flag.set(true);
                }
                value.setPressed(false);
            });
            // 操作按钮
            actionButtons.forEach(bt -> {
                if (bt.isHovered() && bt.isPressed()) {
                    this.handleActionOperation(bt, button, flag);
                }
                bt.setPressed(false);
            });
        }

        keyManager.mouseReleased(button, mouseX, mouseY);
        return flag.get() ? flag.get() : super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        keyManager.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        keyManager.keyPressed(keyCode);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        keyManager.keyReleased(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }


    private void handleActionOperation(OperationButton bt, int button, AtomicBoolean flag) {
        if (bt.getOperation() == 100) {
            toggleMode();
            flag.set(true);
        } else if (bt.getOperation() == 101) {
            if (this.selectMode) openRecipeSelectScreen(); else openBrushItemSelector();
            flag.set(true);
        } else if (bt.getOperation() == 102 && bt instanceof CyclingTextureButton textureButton) {
            textureButton.cycleTexture();
            this.scriptType = textureButton.getCurrentTextureIndex();
            flag.set(true);
        } else if (bt.getOperation() == 103) {
            generateScript();
            flag.set(true);
        }
    }


    private boolean isSlotAvailable(int slotIndex) {
        return this.menu.getAvailableSlots().contains(slotIndex);
    }

    private void openItemSelectScreen(int slotIndex) {
        ItemStack defaultItem = this.menu.getSlotItem(slotIndex);
        if (defaultItem.isEmpty()) {
            defaultItem = new ItemStack(Items.AIR);
        }

        this.minecraft.setScreen(new ItemSelectScreen(
                this,
                (itemStack) -> this.menu.getSlot(slotIndex).set(itemStack.copy()),
                defaultItem
        ));
    }

    // 添加配方选择相关方法
    private void openRecipeSelectScreen() {
        this.minecraft.setScreen(new RecipeSelectScreen(this, this::onRecipeSelected));
    }

    private void onRecipeSelected(Recipe<?> recipe) {
        // 将选中的配方填充到输入输出槽中
        this.fillRecipeIntoSlots(recipe);
        // 返回当前界面
        this.minecraft.setScreen(this);
    }

    private void fillRecipeIntoSlots(Recipe<?> recipe) {
        // 清空现有槽位
        for (int i = 0; i < 82; i++) {
            this.menu.getSlot(i).set(ItemStack.EMPTY);
        }

        ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
        if (!result.isEmpty()) {
            // 设置输出槽
            this.menu.getSlot(81).set(result.copy());
        }

        // 根据具体配方类型填充输入槽并更新参数

        if (recipe instanceof ShapedTableCraftingRecipe shapedRecipe) {
            fillShapedTableRecipe(shapedRecipe);
        } else if (recipe instanceof ShapelessTableCraftingRecipe shapelessRecipe) {
            fillShapelessTableRecipe(shapelessRecipe);
        } else if (recipe instanceof ShapedRecipe shapedRecipe) {
            fillVanillaShapedRecipe(shapedRecipe);
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            fillVanillaShapelessRecipe(shapelessRecipe);
        } else if (recipe instanceof ExtremeSmithingRecipe extremeSmithingRecipe) {
            fillExtremeSmithingRecipe(extremeSmithingRecipe);
        } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
            fillSmeltingRecipe(smeltingRecipe);
        } else if (recipe instanceof BlastingRecipe blastingRecipe) {
            fillBlastingRecipe(blastingRecipe);
        }
    }

    // 填充无尽有序工作台配方
    private void fillShapedTableRecipe(ShapedTableCraftingRecipe recipe) {
        this.menu.switchCategory(RecipeTypes.valueOf(recipe.getTier() + 10));
        fillShapedRecipe(recipe.getIngredients(), recipe.getWidth(), recipe.getHeight());
    }

    // 填充无尽无序工作台配方
    private void fillShapelessTableRecipe(ShapelessTableCraftingRecipe recipe) {
        this.menu.switchCategory(RecipeTypes.valueOf(recipe.getTier() + 10));
        fillShapelessRecipe(recipe.getIngredients());
    }

    // 填充原版有序工作台配方
    private void fillVanillaShapedRecipe(ShapedRecipe recipe) {
        this.menu.switchCategory(RecipeTypes.VANILLA_CRAFTING);
        fillShapedRecipe(recipe.getIngredients(), recipe.getWidth(), recipe.getHeight());
    }

    // 填充原版无序工作台配方
    private void fillVanillaShapelessRecipe(ShapelessRecipe recipe) {
        this.menu.switchCategory(RecipeTypes.VANILLA_CRAFTING);
        fillShapelessRecipe(recipe.getIngredients());
    }

    // 填充无尽锻造配方
    private void fillExtremeSmithingRecipe(ExtremeSmithingRecipe recipe) {
        this.menu.switchCategory(RecipeTypes.VANILLA_SMITHING);
        try {
            this.menu.getSlot(39).set(recipe.template.getItems()[0]); // 模板槽位
            this.menu.getSlot(40).set(recipe.base.getItems()[0]);     // 基础物品槽位
            this.menu.getSlot(41).set(recipe.additions.getItems()[0]); // 添加物品槽位
        } catch (Exception e) {
            // 忽略异常
        }
    }

    // 填充熔炉配方
    private void fillSmeltingRecipe(SmeltingRecipe recipe) {
        this.menu.switchCategory(RecipeTypes.VANILLA_FURNACE);
        fillSingleSlotRecipe(recipe.getIngredients().get(0));
    }

    // 填充高炉配方
    private void fillBlastingRecipe(BlastingRecipe recipe) {
        this.menu.switchCategory(RecipeTypes.VANILLA_FURNACE);
        fillSingleSlotRecipe(recipe.getIngredients().get(0));
    }

    // 填充有序配方的通用方法
    private void fillShapedRecipe(NonNullList<Ingredient> ingredients, int width, int height) {
        try {
            // 计算起始位置（居中）
            int startRow = (9 - height) / 2;
            int startCol = (9 - width) / 2;

            int index = 0;
            for (int y = 0; y < height && y < 9; y++) {
                for (int x = 0; x < width && x < 9; x++) {
                    if (index < ingredients.size()) {
                        Ingredient ingredient = ingredients.get(index);
                        if (!ingredient.isEmpty()) {
                            ItemStack[] items = ingredient.getItems();
                            if (items.length > 0) {
                                int slotIndex = (startRow + y) * 9 + (startCol + x);
                                if (this.isSlotAvailable(slotIndex)) {
                                    this.menu.getSlot(slotIndex).set(items[0].copy());
                                }
                            }
                        }
                    }
                    index++;
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    // 填充无序配方的通用方法
    private void fillShapelessRecipe(java.util.List<Ingredient> ingredients) {
        try {
            int slotIndex = 0;
            for (Ingredient ingredient : ingredients) {
                if (!ingredient.isEmpty()) {
                    ItemStack[] items = ingredient.getItems();
                    if (items.length > 0 && slotIndex < this.menu.getAvailableSlots().size() - 1) { // -1 for output slot
                        int actualSlot = this.menu.getAvailableSlots().get(slotIndex);
                        if (actualSlot != 81) { // Skip output slot
                            this.menu.getSlot(actualSlot).set(items[0].copy());
                            slotIndex++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    // 填充单槽位配方的通用方法
    private void fillSingleSlotRecipe(Ingredient ingredient) {
        try {
            if (!ingredient.isEmpty()) {
                ItemStack[] items = ingredient.getItems();
                if (items.length > 0) {
                    // 放在可用的第一个槽位
                    if (!this.menu.getAvailableSlots().isEmpty()) {
                        int slotIndex = this.menu.getAvailableSlots().get(0);
                        if (slotIndex != 81) { // Skip output slot
                            this.menu.getSlot(slotIndex).set(items[0].copy());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    private void generateScript() {
        if (!this.menu.slots.isEmpty() && !this.menu.getSlotItem(81).isEmpty()) {
            String fileName = "generated_recipe";

            if (Screen.hasShiftDown()) {
                // 如果按住Shift键，则允许自定义文件名
                Minecraft.getInstance().setScreen(new StringInputScreen(this,
                        Text.i18n("gui.avaritia.recipe_generator.enter_filename").setShadow(true),
                        Text.i18n("gui.avaritia.recipe_generator.filename_prompt"),
                        "", "generated_recipe", input -> {
                    if (!input.isEmpty()) {
                        doGenerateScript(input);
                    }
                }));
            } else {
                doGenerateScript(fileName);
            }
        }
    }

    private void doGenerateScript(String fileName) {
        switch (scriptType) {
            case 1: // KubeJS
                KubeJsUtils.exportTableJS(this.menu, true, 4, true, fileName);
                break;
            case 2: // CRT
                CrtUtils.exportTableZS(this.menu, true, 4, true, fileName);
                break;
        }
    }
}
