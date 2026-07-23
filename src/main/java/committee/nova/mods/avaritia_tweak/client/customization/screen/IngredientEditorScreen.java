package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final int MODE_RAIL_WIDTH = 84;

    private final Screen previous;
    private final Consumer<Optional<IngredientSpec>> onSave;
    private boolean tagMode;
    private String idText = "";
    private String nbtText = "";
    private EditBox idBox;
    private EditBox nbtBox;
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
        Layout layout = layout();
        int contentX = layout.left + MODE_RAIL_WIDTH + 12;
        int contentWidth = layout.width - MODE_RAIL_WIDTH - 22;
        int fieldY = layout.top + 66;
        int browseWidth = this.tagMode ? 0 : Math.min(78, Math.max(54, contentWidth / 4));
        int idWidth = contentWidth - (this.tagMode ? 0 : browseWidth + 5);

        this.idBox = new EditBox(this.font, contentX, fieldY, idWidth, 20,
                Component.translatable("gui.avaritia_tweak.resource_id"));
        this.idBox.setMaxLength(256);
        this.idBox.setValue(this.idText);
        this.idBox.setResponder(value -> this.idText = value);
        this.addRenderableWidget(this.idBox);

        if (!this.tagMode) {
            this.addRenderableWidget(EditorButton.builder(
                            Component.translatable("gui.avaritia_tweak.browse_items"),
                            button -> Minecraft.getInstance().setScreen(new RegistryItemSelectScreen(this,
                                    id -> this.idText = id.toString())))
                    .bounds(contentX + idWidth + 5, fieldY, browseWidth, 20)
                    .style(EditorButton.Style.QUIET).build());
            this.nbtBox = new EditBox(this.font, contentX, fieldY + 42, contentWidth, 20,
                    Component.translatable("gui.avaritia_tweak.strict_nbt"));
            this.nbtBox.setMaxLength(4096);
            this.nbtBox.setValue(this.nbtText);
            this.nbtBox.setResponder(value -> this.nbtText = value);
            this.addRenderableWidget(this.nbtBox);
        }

        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.ingredient_item"),
                        button -> setMode(false))
                .bounds(layout.left + 9, layout.top + 62, MODE_RAIL_WIDTH - 18, 24)
                .style(EditorButton.Style.TAB).selected(!this.tagMode).build());
        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.ingredient_tag"),
                        button -> setMode(true))
                .bounds(layout.left + 9, layout.top + 92, MODE_RAIL_WIDTH - 18, 24)
                .style(EditorButton.Style.TAB).selected(this.tagMode).build());

        int actionY = layout.top + layout.height - 30;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.clear"),
                        button -> clear())
                .bounds(layout.left + 9, actionY, 74, 20)
                .style(EditorButton.Style.DANGER).build());
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

    private void setMode(boolean tagMode) {
        this.tagMode = tagMode;
        this.error = "";
        rebuild();
    }

    private void clear() {
        this.onSave.accept(Optional.empty());
        Minecraft.getInstance().setScreen(this.previous);
    }

    private void save() {
        ResourceLocation id = ResourceLocation.tryParse(this.idText.strip());
        if (id == null) {
            this.error = Component.translatable("gui.avaritia_tweak.error.invalid_resource_id").getString();
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
                this.error = exception.getMessage() == null
                        ? Component.translatable("gui.avaritia_tweak.error.invalid_snbt").getString()
                        : exception.getMessage();
                return;
            }
        }
        Minecraft.getInstance().setScreen(this.previous);
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height,
                EditorTheme.AVARITIA_CYAN);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.AVARITIA_GOLD, false);
        graphics.drawString(this.font,
                Component.translatable("gui.avaritia_tweak.ingredient_editor.subtitle"),
                layout.left + 10, layout.top + 25, EditorTheme.TEXT_MUTED, false);

        graphics.fill(layout.left + 2, layout.top + 40,
                layout.left + MODE_RAIL_WIDTH, layout.top + layout.height - 38,
                EditorTheme.PANEL_DARK);
        graphics.fill(layout.left + MODE_RAIL_WIDTH, layout.top + 40,
                layout.left + MODE_RAIL_WIDTH + 1, layout.top + layout.height - 38,
                EditorTheme.BORDER);
        int contentX = layout.left + MODE_RAIL_WIDTH + 12;
        int contentWidth = layout.width - MODE_RAIL_WIDTH - 22;
        EditorTheme.renderCanvasGrid(graphics, contentX - 5, layout.top + 45,
                contentWidth + 10, layout.height - 88);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.resource_id"),
                contentX, layout.top + 54, EditorTheme.TEXT_MUTED, false);
        if (!this.tagMode) {
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.strict_nbt"),
                    contentX, layout.top + 96, EditorTheme.TEXT_MUTED, false);
        } else {
            graphics.drawWordWrap(this.font,
                    Component.translatable("gui.avaritia_tweak.ingredient_tag.help"),
                    contentX, layout.top + 100, contentWidth, EditorTheme.TEXT_FAINT);
        }
        if (!this.error.isEmpty()) {
            graphics.drawString(this.font, ScreenText.fit(this.font, this.error, contentWidth),
                    contentX, layout.top + layout.height - 48, EditorTheme.ERROR, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 480, 240);
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
