package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class EntryTypeSelectScreen extends ThemedEditorScreen {
    private static final int COLUMN_GAP = 6;
    private static final int ROW_GAP = 3;
    private static final int CHOICE_HEIGHT = 20;
    private static final List<EntryKind> RECIPE_KINDS = kinds(true);
    private static final List<EntryKind> SINGULARITY_KINDS = kinds(false);

    private final Screen previous;
    private final Consumer<EntryKind> onSelected;

    EntryTypeSelectScreen(Screen previous, Consumer<EntryKind> onSelected) {
        super(Component.translatable("gui.avaritia_tweak.entry_type_select.title"));
        this.previous = Objects.requireNonNull(previous, "previous");
        this.onSelected = Objects.requireNonNull(onSelected, "onSelected");
    }

    @Override
    protected void init() {
        Layout layout = layout();
        addChoices(layout, RECIPE_KINDS, layout.recipeY);
        addChoices(layout, SINGULARITY_KINDS, layout.singularityY);
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                        button -> onClose())
                .bounds(layout.innerX + layout.innerWidth - 96, layout.actionY, 96, 20)
                .style(EditorButton.Style.QUIET).build());
    }

    private void addChoices(Layout layout, List<EntryKind> kinds, int top) {
        for (int index = 0; index < kinds.size(); index++) {
            EntryKind kind = kinds.get(index);
            int column = index % 2;
            int row = index / 2;
            int x = layout.innerX + column * (layout.columnWidth + COLUMN_GAP);
            int y = top + row * (CHOICE_HEIGHT + ROW_GAP);
            this.addRenderableWidget(EditorButton.builder(kindLabel(kind), button -> select(kind))
                    .bounds(x, y, layout.columnWidth, CHOICE_HEIGHT)
                    .style(EditorButton.Style.DEFAULT).build());
        }
    }

    private void select(EntryKind kind) {
        this.onSelected.accept(kind);
        Minecraft.getInstance().setScreen(this.previous);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        renderThemedBackground(graphics, mouseX, mouseY, partialTick);
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height,
                EditorTheme.AVARITIA_RED);
        graphics.drawString(this.font, this.title, layout.innerX, layout.top + 11,
                EditorTheme.AVARITIA_GOLD, false);
        graphics.drawString(this.font, ScreenText.fit(this.font,
                        Component.translatable("gui.avaritia_tweak.entry_type_select.subtitle").getString(),
                        layout.innerWidth),
                layout.innerX, layout.top + 25, EditorTheme.TEXT_MUTED, false);

        renderGroup(graphics, layout, layout.recipePanelTop, layout.recipePanelBottom,
                layout.recipeLabelY, "gui.avaritia_tweak.entry_type_select.recipes",
                EditorTheme.AVARITIA_CYAN);
        renderGroup(graphics, layout, layout.singularityPanelTop, layout.singularityPanelBottom,
                layout.singularityLabelY, "gui.avaritia_tweak.entry_type_select.singularities",
                EditorTheme.AVARITIA_GOLD);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGroup(GuiGraphics graphics, Layout layout, int top, int bottom,
                             int labelY, String labelKey, int accent) {
        graphics.fill(layout.innerX, top, layout.innerX + layout.innerWidth, bottom,
                EditorTheme.BORDER_DARK);
        graphics.fill(layout.innerX + 1, top + 1,
                layout.innerX + layout.innerWidth - 1, bottom - 1, EditorTheme.PANEL);
        graphics.fill(layout.innerX + 1, top + 1, layout.innerX + 3, bottom - 1, accent);
        graphics.drawString(this.font, Component.translatable(labelKey),
                layout.innerX + 8, labelY, accent, false);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private Layout layout() {
        return layout(this.width, this.height);
    }

    static Layout layout(int screenWidth, int screenHeight) {
        EditorUiScale.Frame frame = EditorUiScale.fit(screenWidth, screenHeight, 520, 300);
        int innerX = frame.left() + 10;
        int innerWidth = Math.max(2, frame.width() - 20);
        int columnWidth = Math.max(1, (innerWidth - COLUMN_GAP) / 2);
        int recipeY = frame.top() + 54;
        int recipeRows = (RECIPE_KINDS.size() + 1) / 2;
        int recipeBottom = recipeY + recipeRows * CHOICE_HEIGHT
                + Math.max(0, recipeRows - 1) * ROW_GAP;
        int singularityPanelTop = recipeBottom + 8;
        int singularityY = singularityPanelTop + 16;
        int singularityRows = (SINGULARITY_KINDS.size() + 1) / 2;
        int singularityBottom = singularityY + singularityRows * CHOICE_HEIGHT
                + Math.max(0, singularityRows - 1) * ROW_GAP;
        return new Layout(frame.left(), frame.top(), frame.width(), frame.height(),
                innerX, innerWidth, columnWidth,
                frame.top() + 38, recipeBottom + 5, frame.top() + 43, recipeY,
                singularityPanelTop, singularityBottom + 5, singularityPanelTop + 5, singularityY,
                frame.top() + frame.height() - 30);
    }

    static List<EntryKind> recipeKinds() {
        return RECIPE_KINDS;
    }

    static List<EntryKind> singularityKinds() {
        return SINGULARITY_KINDS;
    }

    private static List<EntryKind> kinds(boolean recipe) {
        return Arrays.stream(EntryKind.values())
                .filter(kind -> kind.isRecipe() == recipe)
                .toList();
    }

    private static Component kindLabel(EntryKind kind) {
        return Component.translatable("gui.avaritia_tweak.kind."
                + kind.name().toLowerCase(java.util.Locale.ROOT));
    }

    record Layout(int left, int top, int width, int height,
                  int innerX, int innerWidth, int columnWidth,
                  int recipePanelTop, int recipePanelBottom, int recipeLabelY, int recipeY,
                  int singularityPanelTop, int singularityPanelBottom,
                  int singularityLabelY, int singularityY, int actionY) {
    }
}
