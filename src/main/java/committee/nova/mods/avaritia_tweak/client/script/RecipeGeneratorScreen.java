package committee.nova.mods.avaritia_tweak.client.script;

import committee.nova.mods.avaritia.api.client.screen.BaseContainerScreen;
import committee.nova.mods.avaritia.api.client.screen.ItemSelectScreen;
import committee.nova.mods.avaritia.api.client.screen.StringInputScreen;
import committee.nova.mods.avaritia.api.client.screen.component.*;
import committee.nova.mods.avaritia.api.client.util.GuiUtils;
import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia_tweak.AvaritiaTweak;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

/**
 * @author cnlimiter
 */
public class RecipeGeneratorScreen extends BaseContainerScreen<RecipeGeneratorMenu> {
    public static final ResourceLocation BACKGROUND = new ResourceLocation(AvaritiaTweak.MOD_ID, "textures/gui/recipe_generator_back.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation(AvaritiaTweak.MOD_ID, "textures/gui/recipe_generator_craft.png");

    private ItemStack brushItem = ItemStack.EMPTY;
    // 脚本生成设置
    private OutType scriptType = OutType.JS; // 1=KubeJS, 2=CRT

    // UI组件
    private Button selectButton;
    private Button brushButton;
    private CycleTextureButton<Boolean> shapedButton;
    private CycleTextureButton<OutType> scriptTypeButton;
    private Button generateButton;

    public RecipeGeneratorScreen(RecipeGeneratorMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void subInit() {
        super.subInit();
        this.leftPos = (this.width - 234) / 2;
        this.topPos = (this.height - 278) / 2;
        // 初始化选项卡按钮
        initTabButtons(this.leftPos, this.topPos);

        // 初始化其他控件
        initControlButtons(this.leftPos, this.topPos);
    }

    private void initTabButtons(int centerX, int centerY) {
        int tabIndex = 0;
        for (RecipeTypes tab : RecipeTypes.values()) {
            this.addRenderableWidget(
                    TextureButton.builder(Component.translatable(tab.name()), BACKGROUND, button -> {
                                switchToTab(tab);
                            })
                            .bounds(centerX + tab.x, centerY + tab.y, 22, 22)
                            .texStart(235,  tabIndex * 23)
                            .xDiffTex(22)
                            .textureSize(512, 512)
                            .build()
            );
            tabIndex++;
        }
    }

    private void initControlButtons(int centerX, int centerY) {
        // 画笔物品按钮
        brushButton = this.addRenderableWidget(
                new BrushButton(centerX + 180, centerY + 3, button -> {
                    if (!this.menu.isBrushMode()) this.menu.setBrushMode(true);
                    openBrushItemSelector();
                })
        );

        selectButton = this.addRenderableWidget(
                new SelectButton(centerX + 204, centerY + 3, button -> {
                    if (this.menu.isBrushMode()) {
                        this.menu.setBrushMode(false);
                        this.brushItem = ItemStack.EMPTY;
                    }
                    openRecipeSelectScreen();
                })
        );

        shapedButton = this.addRenderableWidget(
                CycleTextureButton.booleanBuilder(Component.translatable("gui.avaritia.recipe_generator.shape")
                        , Component.translatable("gui.avaritia.recipe_generator.shapeless")
                        )
                        .withInitialValue(true)
                        .bounds(centerX + 180, centerY + 15 + 4 * 18, 22, 24)
                        .texStart(0, 454)
                        .xDiffTex(23)
                        .yDiffTex(25)
                        .textureSize(512, 512)
                        .create(BACKGROUND,
                                Component.literal(""), (button, b) -> {
                                    this.menu.setShaped(b);
                                })
        );


        // 脚本类型选择按钮
        scriptTypeButton =  this.addRenderableWidget(
                CycleTextureButton.builder(OutType::getName)
                        .withValues(OutType.values())
                        .withInitialValue(OutType.JS)
                        .bounds(centerX + 180, centerY + 155, 22, 24)
                        .texStart(0, 404)
                        .xDiffTex(23)
                        .yDiffTex(25)
                        .textureSize(512, 512)
                        .create(BACKGROUND,
                                Component.literal(""), (button, out) -> {
                                    scriptType = out;
                                })
        );

        // 生成按钮
        generateButton = this.addRenderableWidget(
                new GenButton(centerX + 204, centerY + 155, button -> generateScript())
        );

    }

    private void switchToTab(RecipeTypes tab) {
            // 通知菜单切换类别
            this.menu.switchCategory(tab);
    }


    private void openBrushItemSelector() {
        this.minecraft.setScreen(new ItemSelectScreen(
                this,
                (itemStack) -> this.brushItem = itemStack.copy(),
                brushItem.isEmpty() ? new ItemStack(Items.AIR) : brushItem
        ));
    }


    private void renderSlots(GuiGraphics graphics) {
        int labelY = 10;
        switch (this.menu.getCategory()) {
            case AVARITIA_EXTREME_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4, this.topPos + labelY + 4, 0, 0, 168, 169, 512, 512);
            }
            case AVARITIA_END_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 18, this.topPos + labelY + 4 + 18, 0, 170, 132, 133, 512, 512);
            }
            case AVARITIA_NETHER_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 2 * 18, this.topPos + labelY + 4 + 2 * 18, 0, 304, 96, 97, 512, 512);
            }
            case AVARITIA_SCULK_CRAFTING, VANILLA_CRAFTING -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 3 * 18, this.topPos + labelY + 4 + 3 * 18, 0, 402, 60, 61, 512, 512);
            }
            case AVARITIA_EXTREME_SMITHING -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 3 * 18, this.topPos + labelY + 4 + 3 * 18, 172, 14, 60, 62, 512, 512);
            }
            case AVARITIA_COMPRESSOR -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 4 * 18, this.topPos + labelY + 4 + 4 * 18, 172, 104, 46, 26, 512, 512);
            }
            case AVARITIA_SINGULARITY -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 4 * 18, this.topPos + labelY + 4 + 4 * 18, 172, 104, 46, 26, 512, 512);
            }
            case VANILLA_SMITHING -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 3 * 18, this.topPos + labelY + 4 + 4 * 18, 172, 77, 60, 26, 512, 512);
            }
            case VANILLA_FURNACE -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 4 * 18, this.topPos + labelY + 4 + 4 * 18, 266, 14, 24, 26, 512, 512);
            }
            case VANILLA_STONECUTTING -> {
                GuiUtils.blit(graphics, WIDGETS, this.leftPos + 4 + 4 * 18, this.topPos + labelY + 4 + 4 * 18, 266, 14, 24, 26, 512, 512);
            }
        }
        GuiUtils.blit(graphics, WIDGETS, this.leftPos + 204, this.topPos + 14 + 4 * 18, 266, 14, 24, 26, 512, 512);//输出槽
        GuiUtils.blit(graphics, WIDGETS, this.leftPos + 3, this.topPos, 171, 0, 170, 13, 512, 512);//标题
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY - 3, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX + 30, this.inventoryLabelY + 112, 4210752, false);
    }

    @Override
    protected void renderBgs(GuiGraphics pGuiGraphics, float pPartialTick, int pX, int pY) {
        pGuiGraphics.blit(BACKGROUND, this.leftPos, this.topPos - 2, 0, 0, 234, 278, 512, 512);
        this.renderSlots(pGuiGraphics);
    }

    @Override
    protected void renderFg(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderFg(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        if (this.menu.isBrushMode()) GuiUtils.blit(pGuiGraphics, BACKGROUND, this.leftPos + 186, this.topPos + 3, 46, 304, 22, 24, 512, 512);
        if(!brushItem.isEmpty()) GuiUtils.renderItem(pGuiGraphics, this.font, brushItem, this.leftPos + 189, this.topPos + 6, false);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.menu.clearAll();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.shapedButton.visible = this.menu.getCategory().isCrafting();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // 左键点击
            // 检查输入槽位区域 (9x9网格)
            int relativeMouseX = (int) (mouseX - (this.leftPos + 8));
            int relativeMouseY = (int) (mouseY - (this.topPos + 18));

            if (relativeMouseX >= 0 && relativeMouseY >= 0) {
                int gridX = relativeMouseX / 18;
                int gridY = relativeMouseY / 18;

                if (gridX < 9 && gridY < 9) {
                    // 点击了输入槽位
                    int slotIndex = gridY * 9 + gridX;
                    if (isSlotAvailable(slotIndex)) {
                        if (this.menu.isBrushMode()) {
                            // 画刷模式：用选定的物品填充槽位
                            Slot slot = this.menu.getSafeSlot(slotIndex);
                            if (slot != null && !brushItem.isEmpty()) {
                                slot.set(brushItem.copy());
                            }
                        } else {
                            // 选择模式：打开物品选择界面
                            openItemSelectScreen(slotIndex);
                        }
                        return true;
                    }
                }
            }

            // 检查输出槽位
            int relativeOutputX = (int) (mouseX - (this.leftPos + 202));
            int relativeOutputY = (int) (mouseY - (this.topPos + 89));

            if (relativeOutputX >= 0 && relativeOutputY >= 0) {
                int outputX = relativeOutputX / 18;
                int outputY = relativeOutputY / 18;

                if (outputX == 0 && outputY == 0) {
                    // 点击了输出槽位
                    openItemSelectScreen(81);
                    return true;
                }
            }
        } else if (button == 1) { // 右键点击清除
            System.out.println(mouseX + " " + mouseY);
            // 检查输入槽位区域 (9x9网格)
            int relativeMouseX = (int) (mouseX - (this.leftPos + 8));
            int relativeMouseY = (int) (mouseY - (this.topPos + 18));

            if (relativeMouseX >= 0 && relativeMouseY >= 0) {
                int gridX = relativeMouseX / 18;
                int gridY = relativeMouseY / 18;

                if (gridX < 9 && gridY < 9) {
                    // 点击了输入槽位
                    int slotIndex = gridY * 9 + gridX;
                    if (isSlotAvailable(slotIndex)) {
                        Slot slot = this.menu.getSafeSlot(slotIndex);
                        if (slot != null && !slot.getItem().isEmpty()) {
                            slot.set(ItemStack.EMPTY);
                        }
                        return true;
                    }
                }
            }

            // 检查输出槽位
            int relativeOutputX = (int) (mouseX - (this.leftPos + 202));
            int relativeOutputY = (int) (mouseY - (this.topPos + 89));

            if (relativeOutputX >= 0 && relativeOutputY >= 0) {
                int outputX = relativeOutputX / 18;
                int outputY = relativeOutputY / 18;

                if (outputX == 0 && outputY == 0) {
                    Slot outputSlot = this.menu.getSafeSlot(81);
                    if (outputSlot != null && !outputSlot.getItem().isEmpty()) {
                        outputSlot.set(ItemStack.EMPTY);
                    }
                    return true;
                }
            }

            // 检查画刷
            int relativeBrushX = (int) (mouseX - (this.leftPos + 186));
            int relativeBrushY = (int) (mouseY - (this.topPos + 3));

            if (relativeBrushX >= 0 && relativeBrushY >= 0
                && relativeBrushX < 22 && relativeBrushY < 24
            ) {
                this.brushItem = ItemStack.EMPTY;
                return true;
            }

        }

        return super.mouseClicked(mouseX, mouseY, button);
    }



    // 简化槽位可用性检查
    private boolean isSlotAvailable(int slotIndex) {
        return this.menu.isSlotValid(slotIndex);
    }

    // 改进打开物品选择界面的方法
    private void openItemSelectScreen(int slotIndex) {
        Slot slot = this.menu.getSafeSlot(slotIndex);
        if (slot == null) {
            return;
        }

        ItemStack defaultItem = slot.getItem();
        if (defaultItem.isEmpty()) {
            defaultItem = new ItemStack(Items.AIR);
        }

        this.minecraft.setScreen(new ItemSelectScreen(
                this,
                (itemStack) -> slot.set(itemStack.copy()),
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
        this.menu.slots.forEach(slot -> slot.set(ItemStack.EMPTY));

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

        ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
        if (!result.isEmpty()) {
            // 设置输出槽
            this.menu.getSafeSlot(81).set(result.copy());
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
            this.menu.getSafeSlot(39).set(recipe.template.getItems()[0]); // 模板槽位
            this.menu.getSafeSlot(40).set(recipe.base.getItems()[0]);     // 基础物品槽位
            this.menu.getSafeSlot(41).set(recipe.additions.getItems()[0]); // 添加物品槽位
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
                                    this.menu.getSafeSlot(slotIndex).set(items[0].copy());
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
                            this.menu.getSafeSlot(actualSlot).set(items[0].copy());
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
                            this.menu.getSafeSlot(slotIndex).set(items[0].copy());
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
            if (Screen.hasShiftDown()) {
                // 如果按住Shift键，则允许自定义文件名
                Minecraft.getInstance().setScreen(new StringInputScreen(this,
                        Text.i18n("gui.avaritia.recipe_generator.enter_filename").setShadow(true),
                        Text.i18n("gui.avaritia.recipe_generator.filename_prompt"),
                        "", "generated_recipe", input -> {
                    if (!input.isEmpty()) {
                        this.menu.doGenerateScript(input, this.scriptType);
                    }
                }));
            } else {
                this.menu.doGenerateScript("generated_recipe", this.scriptType);
            }
        }
    }
}
