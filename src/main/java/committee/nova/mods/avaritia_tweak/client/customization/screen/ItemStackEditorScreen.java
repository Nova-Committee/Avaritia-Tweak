package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public final class ItemStackEditorScreen extends Screen {
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
        int left = this.width / 2 - 140;
        int top = this.height / 2 - 78;
        this.idBox = field(left + 70, top + 24, 140, this.idText, 256);
        this.countBox = field(left + 70, top + 52, 60, this.countText, 10);
        this.nbtBox = field(left + 70, top + 80, 200, this.nbtText, 4096);
        this.idBox.setResponder(value -> this.idText = value);
        this.countBox.setResponder(value -> this.countText = value);
        this.nbtBox.setResponder(value -> this.nbtText = value);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.browse_items"),
                button -> Minecraft.getInstance().setScreen(new RegistryItemSelectScreen(this,
                        id -> this.idText = id.toString())))
                .bounds(left + 216, top + 24, 54, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.save"),
                button -> save()).bounds(left + 70, top + 112, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                button -> onClose()).bounds(left + 180, top + 112, 90, 20).build());
        this.setInitialFocus(this.idBox);
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        this.addRenderableWidget(box);
        return box;
    }

    private void save() {
        ResourceLocation id = ResourceLocation.tryParse(this.idText.strip());
        if (id == null) {
            this.error = "Invalid resource ID";
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
            this.error = exception.getMessage() == null ? "Invalid count or SNBT" : exception.getMessage();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - 140;
        int top = this.height / 2 - 78;
        graphics.fill(left - 8, top - 8, left + 278, top + 144, 0xee171a21);
        graphics.drawString(this.font, this.title, left, top, 0xfff0c66a, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.resource_id"),
                left, top + 30, 0xffaeb6c6, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.count"),
                left, top + 58, 0xffaeb6c6, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.nbt"),
                left, top + 86, 0xffaeb6c6, false);
        if (!this.error.isEmpty()) {
            graphics.drawString(this.font, this.error, left, top + 102, 0xffff6b6b, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }
}
