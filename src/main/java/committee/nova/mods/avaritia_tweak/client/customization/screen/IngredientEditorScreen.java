package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
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

public final class IngredientEditorScreen extends Screen {
    private final Screen previous;
    private final Consumer<Optional<IngredientSpec>> onSave;
    private boolean tagMode;
    private String idText = "";
    private String nbtText = "";
    private EditBox idBox;
    private EditBox nbtBox;
    private Button modeButton;
    private Button itemBrowserButton;
    private String error = "";

    public IngredientEditorScreen(Screen previous, Optional<IngredientSpec> initial,
                                  Consumer<Optional<IngredientSpec>> onSave) {
        super(Component.translatable("gui.avaritia_tweak.ingredient_editor"));
        this.previous = previous;
        this.onSave = onSave;
        this.tagMode = initial.filter(IngredientSpec.Tag.class::isInstance).isPresent();
        initial.ifPresent(value -> {
            if (value instanceof IngredientSpec.Item item) {
                this.idText = item.itemId().toString();
                this.nbtText = item.strictNbt().map(Object::toString).orElse("");
            } else {
                this.idText = ((IngredientSpec.Tag) value).tagId().toString();
            }
        });
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 140;
        int top = this.height / 2 - 70;
        this.idBox = new EditBox(this.font, left + 70, top + 24, 140, 20,
                Component.translatable("gui.avaritia_tweak.resource_id"));
        this.nbtBox = new EditBox(this.font, left + 70, top + 52, 200, 20,
                Component.translatable("gui.avaritia_tweak.strict_nbt"));
        this.nbtBox.setMaxLength(4096);
        this.idBox.setValue(this.idText);
        this.idBox.setResponder(value -> this.idText = value);
        this.nbtBox.setValue(this.nbtText);
        this.nbtBox.setResponder(value -> this.nbtText = value);
        this.addRenderableWidget(this.idBox);
        this.addRenderableWidget(this.nbtBox);
        this.itemBrowserButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.avaritia_tweak.browse_items"),
                button -> Minecraft.getInstance().setScreen(new RegistryItemSelectScreen(this,
                        id -> this.idText = id.toString())))
                .bounds(left + 216, top + 24, 54, 20).build());
        this.modeButton = this.addRenderableWidget(Button.builder(modeLabel(), button -> {
            this.tagMode = !this.tagMode;
            this.nbtBox.visible = !this.tagMode;
            this.itemBrowserButton.visible = !this.tagMode;
            this.modeButton.setMessage(modeLabel());
        }).bounds(left, top + 24, 62, 20).build());
        this.nbtBox.visible = !this.tagMode;
        this.itemBrowserButton.visible = !this.tagMode;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.save"),
                button -> save()).bounds(left, top + 88, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.clear"), button -> {
            this.onSave.accept(Optional.empty());
            Minecraft.getInstance().setScreen(this.previous);
        }).bounds(left + 90, top + 88, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                button -> onClose()).bounds(left + 180, top + 88, 90, 20).build());
        this.setInitialFocus(this.idBox);
    }

    private void save() {
        ResourceLocation id = ResourceLocation.tryParse(this.idText.strip());
        if (id == null) {
            this.error = "Invalid resource ID";
            return;
        }
        if (this.tagMode) {
            this.onSave.accept(Optional.of(new IngredientSpec.Tag(id)));
        } else {
            String rawNbt = this.nbtText.strip();
            try {
                Optional<CompoundTag> nbt = rawNbt.isEmpty()
                        ? Optional.empty()
                        : Optional.of(TagParser.parseTag(rawNbt));
                this.onSave.accept(Optional.of(new IngredientSpec.Item(id, nbt)));
            } catch (Exception exception) {
                this.error = exception.getMessage() == null ? "Invalid SNBT" : exception.getMessage();
                return;
            }
        }
        Minecraft.getInstance().setScreen(this.previous);
    }

    private Component modeLabel() {
        return Component.translatable(this.tagMode
                ? "gui.avaritia_tweak.ingredient_tag"
                : "gui.avaritia_tweak.ingredient_item");
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - 140;
        int top = this.height / 2 - 70;
        graphics.fill(left - 8, top - 8, left + 278, top + 120, 0xee171a21);
        graphics.drawString(this.font, this.title, left, top, 0xfff0c66a, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.resource_id"),
                left + 70, top + 14, 0xffaeb6c6, false);
        if (!this.tagMode) {
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.strict_nbt"),
                    left + 70, top + 43, 0xffaeb6c6, false);
        }
        if (!this.error.isEmpty()) {
            graphics.drawString(this.font, this.error, left, top + 76, 0xffff6b6b, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }
}
