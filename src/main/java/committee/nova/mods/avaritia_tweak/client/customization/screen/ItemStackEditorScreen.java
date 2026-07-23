package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public final class ItemStackEditorScreen extends Screen {
    private static final int PREVIEW_RAIL_WIDTH = 96;

    private final Screen previous;
    private final Consumer<ItemStackSpec> onSave;
    private String idText;
    private String countText;
    private String nbtText;
    private EditBox idBox;
    private EditBox countBox;
    private EditBox nbtBox;
    private String error = "";

    public ItemStackEditorScreen(Screen previous, ItemStackSpec initial, Consumer<ItemStackSpec> onSave) {
        super(Component.translatable("gui.avaritia_tweak.stack_editor"));
        this.previous = previous;
        this.onSave = onSave;
        this.idText = initial.itemId().toString();
        this.countText = Integer.toString(initial.count());
        this.nbtText = initial.nbt().map(Object::toString).orElse("");
    }

    @Override
    protected void init() {
        Layout layout = layout();
        int contentX = layout.left + PREVIEW_RAIL_WIDTH + 12;
        int contentWidth = layout.width - PREVIEW_RAIL_WIDTH - 22;
        int browseWidth = Math.min(78, Math.max(54, contentWidth / 4));
        int idWidth = contentWidth - browseWidth - 5;
        this.idBox = field(contentX, layout.top + 62, idWidth, this.idText, 256,
                Component.translatable("gui.avaritia_tweak.resource_id"));
        this.countBox = field(contentX, layout.top + 102, Math.min(92, contentWidth),
                this.countText, 10, Component.translatable("gui.avaritia_tweak.count"));
        this.nbtBox = field(contentX, layout.top + 142, contentWidth,
                this.nbtText, 4096, Component.translatable("gui.avaritia_tweak.nbt"));
        this.idBox.setResponder(value -> this.idText = value);
        this.countBox.setResponder(value -> this.countText = value);
        this.nbtBox.setResponder(value -> this.nbtText = value);

        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.browse_items"),
                        button -> Minecraft.getInstance().setScreen(new RegistryItemSelectScreen(this,
                                id -> this.idText = id.toString())))
                .bounds(contentX + idWidth + 5, layout.top + 62, browseWidth, 20)
                .style(EditorButton.Style.QUIET).build());

        int actionY = layout.top + layout.height - 30;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                        button -> onClose())
                .bounds(layout.left + layout.width - 174, actionY, 78, 20)
                .style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.save"),
                        button -> save())
                .bounds(layout.left + layout.width - 90, actionY, 80, 20)
                .style(EditorButton.Style.PRIMARY).build());
        this.setInitialFocus(this.idBox);
    }

    private EditBox field(int x, int y, int width, String value, int maxLength, Component narration) {
        EditBox box = new EditBox(this.font, x, y, width, 20, narration);
        box.setMaxLength(maxLength);
        box.setValue(value);
        this.addRenderableWidget(box);
        return box;
    }

    private void save() {
        ResourceLocation id = ResourceLocation.tryParse(this.idText.strip());
        if (id == null) {
            this.error = Component.translatable("gui.avaritia_tweak.error.invalid_resource_id").getString();
            return;
        }
        try {
            int count = Integer.parseInt(this.countText.strip());
            String rawNbt = this.nbtText.strip();
            Optional<CompoundTag> nbt = rawNbt.isEmpty()
                    ? Optional.empty()
                    : Optional.of(TagParser.parseTag(rawNbt));
            this.onSave.accept(new ItemStackSpec(id, count, nbt));
            Minecraft.getInstance().setScreen(this.previous);
        } catch (Exception exception) {
            this.error = exception.getMessage() == null
                    ? Component.translatable("gui.avaritia_tweak.error.invalid_stack").getString()
                    : exception.getMessage();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height,
                EditorTheme.AVARITIA_RED);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.AVARITIA_GOLD, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.stack_editor.subtitle"),
                layout.left + 10, layout.top + 25, EditorTheme.TEXT_MUTED, false);

        graphics.fill(layout.left + 2, layout.top + 40,
                layout.left + PREVIEW_RAIL_WIDTH, layout.top + layout.height - 38,
                EditorTheme.PANEL_DARK);
        graphics.fill(layout.left + PREVIEW_RAIL_WIDTH, layout.top + 40,
                layout.left + PREVIEW_RAIL_WIDTH + 1, layout.top + layout.height - 38,
                EditorTheme.BORDER);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.avaritia_tweak.stack_editor.preview"),
                layout.left + PREVIEW_RAIL_WIDTH / 2, layout.top + 55, EditorTheme.TEXT_MUTED);
        int slotX = layout.left + PREVIEW_RAIL_WIDTH / 2 - 18;
        int slotY = layout.top + 76;
        EditorTheme.renderSlot(graphics, slotX, slotY, 36, 36,
                mouseX >= slotX && mouseX < slotX + 36 && mouseY >= slotY && mouseY < slotY + 36);
        ItemStack preview = previewStack();
        if (!preview.isEmpty()) {
            graphics.renderItem(preview, slotX + 10, slotY + 10);
            graphics.renderItemDecorations(this.font, preview, slotX + 10, slotY + 10);
        }
        graphics.drawCenteredString(this.font, ScreenText.fit(this.font, this.idText,
                        PREVIEW_RAIL_WIDTH - 12),
                layout.left + PREVIEW_RAIL_WIDTH / 2, slotY + 45, EditorTheme.TEXT_FAINT);

        int contentX = layout.left + PREVIEW_RAIL_WIDTH + 12;
        int contentWidth = layout.width - PREVIEW_RAIL_WIDTH - 22;
        EditorTheme.renderCanvasGrid(graphics, contentX - 5, layout.top + 45,
                contentWidth + 10, layout.height - 88);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.resource_id"),
                contentX, layout.top + 50, EditorTheme.TEXT_MUTED, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.count"),
                contentX, layout.top + 90, EditorTheme.TEXT_MUTED, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.nbt"),
                contentX, layout.top + 130, EditorTheme.TEXT_MUTED, false);
        if (!this.error.isEmpty()) {
            graphics.drawString(this.font, ScreenText.fit(this.font, this.error, contentWidth),
                    contentX, layout.top + layout.height - 48, EditorTheme.ERROR, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private ItemStack previewStack() {
        ResourceLocation id = ResourceLocation.tryParse(this.idText.strip());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        try {
            stack.setCount(Math.max(1, Integer.parseInt(this.countText.strip())));
        } catch (NumberFormatException ignored) {
            stack.setCount(1);
        }
        return stack;
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 500, 270);
        int panelWidth = frame.width();
        int panelHeight = frame.height();
        int left = frame.left();
        int top = frame.top();
        return new Layout(left, top, panelWidth, panelHeight);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private record Layout(int left, int top, int width, int height) {
    }
}
