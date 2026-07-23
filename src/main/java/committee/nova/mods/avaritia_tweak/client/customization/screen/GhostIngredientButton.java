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
        super(x, y, 18, 18, Component.translatable("gui.avaritia_tweak.ghost_ingredient"));
        this.value = value;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.accept(this);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        EditorTheme.renderSlot(graphics, this.getX(), this.getY(), this.width, this.height,
                this.isHoveredOrFocused());
        Optional<IngredientSpec> current = this.value.get();
        if (current.isEmpty()) {
            graphics.drawCenteredString(Minecraft.getInstance().font, "+",
                    this.getX() + 9, this.getY() + 5, EditorTheme.TEXT_MUTED);
        } else if (current.get() instanceof IngredientSpec.Item item) {
            Item registered = ForgeRegistries.ITEMS.getValue(item.itemId());
            ItemStack stack = registered == null ? ItemStack.EMPTY : new ItemStack(registered);
            item.strictNbt().ifPresent(stack::setTag);
            graphics.renderItem(stack, this.getX() + 1, this.getY() + 1);
        } else {
            graphics.drawCenteredString(Minecraft.getInstance().font, "#",
                    this.getX() + 9, this.getY() + 5, EditorTheme.AVARITIA_CYAN);
        }
        if (current.filter(IngredientSpec.Item.class::isInstance)
                .map(IngredientSpec.Item.class::cast).flatMap(IngredientSpec.Item::strictNbt).isPresent()) {
            graphics.fill(this.getX() + 13, this.getY() + 2, this.getX() + 16, this.getY() + 5,
                    EditorTheme.AVARITIA_RED);
        }
        if (this.isHovered()) {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(current.map(GhostIngredientButton::describe).orElse("Empty ghost ingredient")),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }

    private static String describe(IngredientSpec ingredient) {
        if (ingredient instanceof IngredientSpec.Tag tag) {
            return "#" + tag.tagId();
        }
        IngredientSpec.Item item = (IngredientSpec.Item) ingredient;
        return item.itemId() + (item.strictNbt().isPresent() ? " (strict NBT)" : "");
    }
}
