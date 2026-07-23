package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GhostIngredientButton extends AbstractButton {
    private final Supplier<Optional<IngredientSpec>> value;
    private final Consumer<GhostIngredientButton> onPress;

    public GhostIngredientButton(int x, int y, Supplier<Optional<IngredientSpec>> value,
                                 Consumer<GhostIngredientButton> onPress) {
        this(x, y, 18, value, onPress);
    }

    public GhostIngredientButton(int x, int y, int size,
                                 Supplier<Optional<IngredientSpec>> value,
                                 Consumer<GhostIngredientButton> onPress) {
        super(x, y, size, size, Component.translatable("gui.avaritia_tweak.ghost_ingredient"));
        this.value = value;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.accept(this);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Optional<IngredientSpec> current = this.value.get();
        renderPreview(graphics, this.getX(), this.getY(), this.width, current,
                this.isHoveredOrFocused());
        if (this.isHovered()) {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(current.map(GhostIngredientButton::describe).orElse("Empty ghost ingredient")),
                    mouseX, mouseY);
        }
    }

    static void renderPreview(GuiGraphics graphics, int x, int y, int size,
                              Optional<IngredientSpec> current, boolean hovered) {
        renderPreview(graphics, x, y, size, current, hovered, true);
    }

    static void renderReadOnlyPreview(GuiGraphics graphics, int x, int y, int size,
                                      Optional<IngredientSpec> current, boolean hovered) {
        renderPreview(graphics, x, y, size, current, hovered, false);
    }

    private static void renderPreview(GuiGraphics graphics, int x, int y, int size,
                                      Optional<IngredientSpec> current, boolean hovered,
                                      boolean showEmptyMark) {
        EditorTheme.renderSlot(graphics, x, y, size, size, hovered);
        if (current.isEmpty()) {
            if (showEmptyMark) {
                renderCompactMark(graphics, x, y, size, false, EditorTheme.TEXT_MUTED);
            }
        } else if (current.get() instanceof IngredientSpec.Item item) {
            Item registered = ForgeRegistries.ITEMS.getValue(item.itemId());
            ItemStack stack = registered == null ? ItemStack.EMPTY : new ItemStack(registered);
            item.strictNbt().ifPresent(stack::setTag);
            renderScaledItem(graphics, x, y, size, stack);
        } else {
            renderCompactMark(graphics, x, y, size, true, EditorTheme.AVARITIA_CYAN);
        }
        if (current.filter(IngredientSpec.Item.class::isInstance)
                .map(IngredientSpec.Item.class::cast).flatMap(IngredientSpec.Item::strictNbt).isPresent()) {
            int marker = Math.max(2, Math.min(3, size / 4));
            graphics.fill(x + size - marker - 2, y + 2,
                    x + size - 2, y + 2 + marker,
                    EditorTheme.AVARITIA_RED);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }

    static String describe(IngredientSpec ingredient) {
        if (ingredient instanceof IngredientSpec.Tag tag) {
            return "#" + tag.tagId();
        }
        IngredientSpec.Item item = (IngredientSpec.Item) ingredient;
        return item.itemId() + (item.strictNbt().isPresent() ? " (strict NBT)" : "");
    }

    private static void renderScaledItem(GuiGraphics graphics, int x, int y, int size, ItemStack stack) {
        float scale = Math.max(1, size - 2) / 16.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1, y + 1, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private static void renderCompactMark(GuiGraphics graphics, int x, int y, int size,
                                          boolean tag, int color) {
        if (size >= 14) {
            graphics.drawCenteredString(Minecraft.getInstance().font, tag ? "#" : "+",
                    x + size / 2, y + (size - 8) / 2, color);
            return;
        }
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        graphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, color);
        graphics.fill(centerX, centerY - 2, centerX + 1, centerY + 3, color);
        if (tag) {
            graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY - 1, color);
        }
    }
}
