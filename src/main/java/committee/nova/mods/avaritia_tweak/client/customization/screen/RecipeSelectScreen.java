package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.importers.AvaritiaRecipeImporter;
import committee.nova.mods.avaritia_tweak.client.customization.importers.RecipeImportResult;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class RecipeSelectScreen extends Screen {
    private final Screen previous;
    private final OutputTarget target;
    private final Consumer<RecipeImportResult> onImported;
    private final AvaritiaRecipeImporter importer = new AvaritiaRecipeImporter();
    private final List<Recipe<?>> allRecipes = new ArrayList<>();
    private final List<Recipe<?>> filteredRecipes = new ArrayList<>();
    private final List<RecipeRow> visibleRows = new ArrayList<>();
    private EditBox searchBox;
    private Recipe<?> selected;
    private int page;
    private String query = "";
    private boolean loaded;
    private boolean rebuildQueued;

    public RecipeSelectScreen(Screen previous, OutputTarget target,
                              Consumer<RecipeImportResult> onImported) {
        super(Component.translatable("gui.avaritia_tweak.recipe_import.title"));
        this.previous = previous;
        this.target = target == OutputTarget.DATAPACK ? OutputTarget.KUBEJS : target;
        this.onImported = onImported;
    }

    @Override
    protected void init() {
        if (!this.loaded) {
            this.loaded = true;
            loadRecipes();
        }
        Layout layout = layout();
        this.visibleRows.clear();
        this.searchBox = new EditBox(this.font, layout.left + 8, layout.top + 58,
                layout.listWidth - 16, 20, Component.translatable("gui.avaritia_tweak.search"));
        this.searchBox.setValue(this.query);
        this.searchBox.setHint(Component.translatable("gui.avaritia_tweak.search"));
        this.searchBox.setResponder(this::filter);
        this.addRenderableWidget(this.searchBox);

        int pageSize = pageSize(layout);
        int maxPage = maxPage(pageSize);
        this.page = Math.min(this.page, maxPage);
        int start = this.page * pageSize;
        for (int index = start; index < Math.min(this.filteredRecipes.size(), start + pageSize); index++) {
            Recipe<?> recipe = this.filteredRecipes.get(index);
            int y = layout.listTop + (index - start) * 22;
            String id = ScreenText.fit(this.font, recipe.getId().toString(), layout.listWidth - 58);
            this.addRenderableWidget(EditorButton.builder(Component.literal(id), button -> {
                        this.selected = recipe;
                        rebuild();
                    }).bounds(layout.left + 34, y, layout.listWidth - 42, 20)
                    .style(EditorButton.Style.LIST).selected(recipe == this.selected).build());
            this.visibleRows.add(new RecipeRow(recipe, layout.left + 10, y + 1));
        }

        int actionY = layout.top + layout.height - 30;
        this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            rebuild();
        }).bounds(layout.left + 8, actionY, 34, 20).style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
            this.page = Math.min(maxPage(pageSize), this.page + 1);
            rebuild();
        }).bounds(layout.left + layout.listWidth - 42, actionY, 34, 20)
                .style(EditorButton.Style.QUIET).build());

        int detailX = layout.detailX;
        int detailWidth = layout.detailWidth;
        int cancelWidth = Math.min(84, detailWidth / 2 - 3);
        EditorButton importButton = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.import_recipe"),
                        button -> importSelected())
                .bounds(detailX + cancelWidth + 5, actionY,
                        detailWidth - cancelWidth - 5, 20)
                .style(EditorButton.Style.PRIMARY).build());
        importButton.active = this.selected != null;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                        button -> onClose())
                .bounds(detailX, actionY, cancelWidth, 20)
                .style(EditorButton.Style.QUIET).build());
        this.setInitialFocus(this.searchBox);
        this.searchBox.setCursorPosition(this.query.length());
    }

    private void loadRecipes() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Minecraft.getInstance().level.getRecipeManager().getRecipes().stream()
                .filter(this.importer::supports)
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                .forEach(this.allRecipes::add);
        this.filteredRecipes.addAll(this.allRecipes);
    }

    private void filter(String value) {
        this.query = value;
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        this.filteredRecipes.clear();
        this.allRecipes.stream()
                .filter(recipe -> normalized.isEmpty()
                        || recipe.getId().toString().toLowerCase(Locale.ROOT).contains(normalized)
                        || result(recipe).getHoverName().getString()
                        .toLowerCase(Locale.ROOT).contains(normalized))
                .forEach(this.filteredRecipes::add);
        this.page = 0;
        this.selected = null;
        if (!this.rebuildQueued) {
            this.rebuildQueued = true;
            Minecraft.getInstance().execute(() -> {
                this.rebuildQueued = false;
                if (Minecraft.getInstance().screen == this) {
                    rebuild();
                }
            });
        }
    }

    private void importSelected() {
        if (this.selected == null || Minecraft.getInstance().level == null) {
            return;
        }
        RecipeImportResult result = this.importer.importRecipe(this.selected, this.target,
                Minecraft.getInstance().level.registryAccess());
        this.onImported.accept(result);
        Minecraft.getInstance().setScreen(this.previous);
    }

    private ItemStack result(Recipe<?> recipe) {
        if (Minecraft.getInstance().level == null) {
            return ItemStack.EMPTY;
        }
        return recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();
        if (mouseX > layout.left + layout.listWidth) {
            return false;
        }
        int pageSize = pageSize(layout);
        this.page = Math.max(0, Math.min(maxPage(pageSize),
                this.page + (delta < 0 ? 1 : -1)));
        rebuild();
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height,
                EditorTheme.AVARITIA_RED);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.AVARITIA_GOLD, false);
        String targetText = this.target.name();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(targetText) - 22,
                layout.top + 9, targetText, EditorTheme.AVARITIA_CYAN);

        EditorTheme.renderSectionHeader(graphics, this.font, layout.left + 2, layout.top + 34,
                layout.listWidth - 2, Component.translatable("gui.avaritia_tweak.recipe_import.recipes"),
                Integer.toString(this.filteredRecipes.size()), EditorTheme.AVARITIA_CYAN);
        EditorTheme.renderPanel(graphics, layout.detailX - 6, layout.top + 34,
                layout.detailWidth + 12, layout.height - 72, EditorTheme.AVARITIA_GOLD);
        EditorTheme.renderCanvasGrid(graphics, layout.detailX - 2, layout.top + 38,
                layout.detailWidth + 4, layout.height - 80);
        EditorTheme.renderSectionHeader(graphics, this.font, layout.detailX - 4, layout.top + 36,
                layout.detailWidth + 8,
                Component.translatable("gui.avaritia_tweak.recipe_import.inspector"), "",
                EditorTheme.AVARITIA_GOLD);
        renderRecipeIcons(graphics, mouseX, mouseY);
        renderSelected(graphics, layout);

        int pageSize = pageSize(layout);
        graphics.drawCenteredString(this.font,
                (this.page + 1) + "/" + (maxPage(pageSize) + 1),
                layout.left + layout.listWidth / 2,
                layout.top + layout.height - 24, EditorTheme.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRecipeIcons(GuiGraphics graphics, int mouseX, int mouseY) {
        for (RecipeRow row : this.visibleRows) {
            EditorTheme.renderSlot(graphics, row.x, row.y, 20, 20,
                    mouseX >= row.x && mouseX < row.x + 20 && mouseY >= row.y && mouseY < row.y + 20);
            ItemStack stack = result(row.recipe);
            graphics.renderItem(stack, row.x + 2, row.y + 2);
        }
    }

    private void renderSelected(GuiGraphics graphics, Layout layout) {
        if (this.selected == null) {
            graphics.drawWordWrap(this.font,
                    Component.translatable("gui.avaritia_tweak.recipe_import.select_help"),
                    layout.detailX, layout.top + 68, layout.detailWidth, EditorTheme.TEXT_MUTED);
            return;
        }
        ItemStack result = result(this.selected);
        int slotX = layout.detailX;
        int slotY = layout.top + 68;
        EditorTheme.renderSlot(graphics, slotX, slotY, 38, 38, false);
        graphics.renderItem(result, slotX + 11, slotY + 11);
        graphics.renderItemDecorations(this.font, result, slotX + 11, slotY + 11);
        graphics.drawString(this.font,
                ScreenText.fit(this.font, result.getHoverName().getString(), layout.detailWidth - 48),
                slotX + 46, slotY + 5, EditorTheme.TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.recipe_import.result"),
                slotX + 46, slotY + 19, EditorTheme.TEXT_FAINT, false);

        int y = slotY + 54;
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.recipe_import.recipe_id"),
                layout.detailX, y, EditorTheme.TEXT_MUTED, false);
        graphics.drawWordWrap(this.font, Component.literal(this.selected.getId().toString()),
                layout.detailX, y + 13, layout.detailWidth, EditorTheme.AVARITIA_CYAN);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.recipe_import.handler"),
                layout.detailX, y + 50, EditorTheme.TEXT_MUTED, false);
        graphics.drawWordWrap(this.font,
                Component.literal(this.selected.getClass().getSimpleName()),
                layout.detailX, y + 63, layout.detailWidth, EditorTheme.SUCCESS);
    }

    private int pageSize(Layout layout) {
        return Math.max(1, (layout.detailsBottom - layout.listTop - 4) / 22);
    }

    private int maxPage(int pageSize) {
        return Math.max(0, (this.filteredRecipes.size() - 1) / pageSize);
    }

    private Layout layout() {
        int panelWidth = Math.min(720, this.width - 12);
        int panelHeight = Math.min(390, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int listWidth = Math.max(210, Math.min(410, panelWidth * 3 / 5));
        int detailX = left + listWidth + 10;
        int detailWidth = panelWidth - listWidth - 18;
        int listTop = top + 84;
        int detailsBottom = top + panelHeight - 38;
        return new Layout(left, top, panelWidth, panelHeight, listWidth,
                detailX, detailWidth, listTop, detailsBottom);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private record RecipeRow(Recipe<?> recipe, int x, int y) {
    }

    private record Layout(int left, int top, int width, int height, int listWidth,
                          int detailX, int detailWidth, int listTop, int detailsBottom) {
    }
}
