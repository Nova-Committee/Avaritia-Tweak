package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
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
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GhostItemStackButton extends AbstractButton {
    private final Supplier<ItemStackSpec> value;
    private final Consumer<GhostItemStackButton> onPress;
    private final SecondaryPressHandler<GhostItemStackButton> onSecondaryPress;

    public GhostItemStackButton(int x, int y, Supplier<ItemStackSpec> value,
                                Consumer<GhostItemStackButton> onPress) {
        this(x, y, value, onPress, null);
    }

    GhostItemStackButton(int x, int y, Supplier<ItemStackSpec> value,
                         Consumer<GhostItemStackButton> onPress,
                         SecondaryPressHandler<GhostItemStackButton> onSecondaryPress) {
        super(x, y, 18, 18, Component.translatable("gui.avaritia_tweak.ghost_result"));
        this.value = value;
        this.onPress = onPress;
        this.onSecondaryPress = onSecondaryPress;
    }

    @Override
    public void onPress() {
        this.onPress.accept(this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && this.active && this.visible
                && this.onSecondaryPress != null && this.isMouseOver(mouseX, mouseY)) {
            this.onSecondaryPress.onPress(this, mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        EditorTheme.renderSlot(graphics, this.getX(), this.getY(), this.width, this.height,
                this.isHoveredOrFocused());
        ItemStackSpec current = this.value.get();
        Item registered = ForgeRegistries.ITEMS.getValue(current.itemId());
        ItemStack stack = registered == null
                ? ItemStack.EMPTY
                : new ItemStack(registered, current.count());
        current.nbt().ifPresent(stack::setTag);
        graphics.renderItem(stack, this.getX() + 1, this.getY() + 1);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, this.getX() + 1, this.getY() + 1);
        if (this.isHovered()) {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(current.itemId() + " × " + current.count()
                            + (current.nbt().isPresent() ? " (NBT)" : "")), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}
