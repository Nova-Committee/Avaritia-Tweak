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
    private Mode mode = Mode.ITEM;
    private IngredientSpec.Choice importedChoice;
    private IngredientSpec.Components importedComponents;
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
        initial.ifPresent(value -> {
            if (value instanceof IngredientSpec.Item item) {
                this.mode = Mode.ITEM;
                this.idText = item.itemId().toString();
                this.nbtText = item.strictNbt().map(Object::toString).orElse("");
            } else if (value instanceof IngredientSpec.Tag tag) {
                this.mode = Mode.TAG;
                this.idText = tag.tagId().toString();
            } else if (value instanceof IngredientSpec.Choice choice) {
                this.mode = Mode.CHOICE;
                this.importedChoice = choice;
            } else if (value instanceof IngredientSpec.Components components) {
                this.mode = Mode.COMPONENTS;
                this.importedComponents = components;
            }
        });
    }

    @Override
    protected void init() {
        Layout layout = layout();
        int contentX = layout.left + MODE_RAIL_WIDTH + 12;
        int contentWidth = layout.width - MODE_RAIL_WIDTH - 22;
        int fieldY = layout.top + 66;
        this.idBox = null;
        this.nbtBox = null;
        if (this.mode.editable()) {
            boolean tagMode = this.mode == Mode.TAG;
            int browseWidth = tagMode ? 0 : Math.min(78, Math.max(54, contentWidth / 4));
            int idWidth = contentWidth - (tagMode ? 0 : browseWidth + 5);

            this.idBox = new EditBox(this.font, contentX, fieldY, idWidth, 20,
                    Component.translatable("gui.avaritia_tweak.resource_id"));
            this.idBox.setMaxLength(256);
            this.idBox.setValue(this.idText);
            this.idBox.setResponder(value -> this.idText = value);
            this.addRenderableWidget(this.idBox);

            if (!tagMode) {
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
        }

        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.ingredient_item"),
                        button -> setMode(Mode.ITEM))
                .bounds(layout.left + 9, layout.top + 62, MODE_RAIL_WIDTH - 18, 24)
                .style(EditorButton.Style.TAB).selected(this.mode == Mode.ITEM).build());
        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.ingredient_tag"),
                        button -> setMode(Mode.TAG))
                .bounds(layout.left + 9, layout.top + 92, MODE_RAIL_WIDTH - 18, 24)
                .style(EditorButton.Style.TAB).selected(this.mode == Mode.TAG).build());
        if (this.importedChoice != null) {
            this.addRenderableWidget(EditorButton.builder(
                            Component.translatable("gui.avaritia_tweak.ingredient_choice"),
                            button -> setMode(Mode.CHOICE))
                    .bounds(layout.left + 9, layout.top + 122, MODE_RAIL_WIDTH - 18, 24)
                    .style(EditorButton.Style.TAB).selected(this.mode == Mode.CHOICE).build());
        }
        if (this.importedComponents != null) {
            this.addRenderableWidget(EditorButton.builder(
                            Component.translatable("gui.avaritia_tweak.ingredient_components"),
                            button -> setMode(Mode.COMPONENTS))
                    .bounds(layout.left + 9, layout.top + 122, MODE_RAIL_WIDTH - 18, 24)
                    .style(EditorButton.Style.TAB).selected(this.mode == Mode.COMPONENTS).build());
        }

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
        if (this.idBox != null) {
            this.setInitialFocus(this.idBox);
        }
    }

    private void setMode(Mode mode) {
        if (!this.mode.editable() && mode != this.mode) {
            this.idText = "";
            this.nbtText = "";
        }
        this.mode = mode;
        this.error = "";
        rebuild();
    }

    private void clear() {
        this.onSave.accept(Optional.empty());
        Minecraft.getInstance().setScreen(this.previous);
    }

    private void save() {
        if (this.mode == Mode.CHOICE && this.importedChoice != null) {
            this.onSave.accept(Optional.of(this.importedChoice));
            Minecraft.getInstance().setScreen(this.previous);
            return;
        }
        if (this.mode == Mode.COMPONENTS && this.importedComponents != null) {
            this.onSave.accept(Optional.of(this.importedComponents));
            Minecraft.getInstance().setScreen(this.previous);
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(this.idText.strip());
        if (id == null) {
            this.error = Component.translatable("gui.avaritia_tweak.error.invalid_resource_id").getString();
            return;
        }
        if (this.mode == Mode.TAG) {
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
        this.setFocused(null);
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
        if (this.mode.editable()) {
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.resource_id"),
                    contentX, layout.top + 54, EditorTheme.TEXT_MUTED, false);
        }
        if (this.mode == Mode.ITEM) {
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.strict_nbt"),
                    contentX, layout.top + 96, EditorTheme.TEXT_MUTED, false);
        } else if (this.mode == Mode.TAG) {
            graphics.drawWordWrap(this.font,
                    Component.translatable("gui.avaritia_tweak.ingredient_tag.help"),
                    contentX, layout.top + 100, contentWidth, EditorTheme.TEXT_FAINT);
        } else if (this.mode == Mode.CHOICE) {
            renderChoice(graphics, contentX, layout.top + 54, contentWidth,
                    layout.top + layout.height - 48);
        } else {
            renderComponents(graphics, contentX, layout.top + 54, contentWidth);
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

    private void renderChoice(GuiGraphics graphics, int x, int y, int width, int bottom) {
        graphics.drawString(this.font,
                Component.translatable("gui.avaritia_tweak.ingredient_choice"),
                x, y, EditorTheme.AVARITIA_GOLD, false);
        graphics.drawWordWrap(this.font,
                Component.translatable("gui.avaritia_tweak.ingredient_choice.help"),
                x, y + 13, width, EditorTheme.TEXT_FAINT);
        int lineY = y + 36;
        for (int index = 0; index < this.importedChoice.alternatives().size()
                && lineY + 9 <= bottom; index++) {
            String value = (index + 1) + ". "
                    + GhostIngredientButton.describe(this.importedChoice.alternatives().get(index));
            graphics.drawString(this.font, ScreenText.fit(this.font, value, width),
                    x, lineY, EditorTheme.TEXT, false);
            lineY += 11;
        }
    }

    private void renderComponents(GuiGraphics graphics, int x, int y, int width) {
        graphics.drawString(this.font,
                Component.translatable("gui.avaritia_tweak.ingredient_components"),
                x, y, EditorTheme.AVARITIA_GOLD, false);
        graphics.drawWordWrap(this.font,
                Component.translatable("gui.avaritia_tweak.ingredient_components.help"),
                x, y + 13, width, EditorTheme.TEXT_FAINT);
        graphics.drawWordWrap(this.font,
                Component.literal(GhostIngredientButton.describe(this.importedComponents)),
                x, y + 42, width, EditorTheme.TEXT);
    }

    private enum Mode {
        ITEM,
        TAG,
        CHOICE,
        COMPONENTS;

        private boolean editable() {
            return this == ITEM || this == TAG;
        }
    }

    private record Layout(int left, int top, int width, int height) {
    }
}
