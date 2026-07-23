package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.importers.AvaritiaRecipeImporter;
import committee.nova.mods.avaritia_tweak.client.customization.importers.RecipeImportResult;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public final class RecipeSelectScreen extends Screen {
    private final Screen previous;
    private final OutputTarget target;
    private final Consumer<RecipeImportResult> onImported;
    private final AvaritiaRecipeImporter importer = new AvaritiaRecipeImporter();
    private final List<Recipe<?>> allRecipes = new ArrayList<>();
    private final List<Recipe<?>> filteredRecipes = new ArrayList<>();
    private final List<RecipeRow> visibleRows = new ArrayList<>();
    private final List<EditorButton> resultWidgets = new ArrayList<>();
    private final DeferredSearchRefresh searchRefresh = new DeferredSearchRefresh();
    private EditBox searchBox;
    private EditorButton previousPage;
    private EditorButton nextPage;
    private EditorButton importButton;
    private Recipe<?> selected;
    private Optional<RecipeImportResult> selectedInspection = Optional.empty();
    private Optional<ResourceLocation> inventoryOutput = Optional.empty();
    private int page;
    private String query = "";
    private boolean loaded;

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
        this.resultWidgets.clear();
        this.visibleRows.clear();
        this.searchBox = new EditBox(this.font, layout.left + 8, layout.top + 58,
                layout.listWidth - 16, 20, Component.translatable("gui.avaritia_tweak.search"));
        this.searchBox.setMaxLength(256);
        this.searchBox.setValue(this.query);
        this.searchBox.setHint(Component.translatable("gui.avaritia_tweak.search"));
        this.searchBox.setResponder(this::filter);
        this.addRenderableWidget(this.searchBox);

        int clearWidth = 42;
        int pickerWidth = Math.max(1, layout.listWidth - 20 - clearWidth);
        this.addRenderableWidget(EditorButton.builder(inventoryFilterLabel(), button -> openInventoryFilter())
                .bounds(layout.left + 8, layout.top + 82, pickerWidth, 20)
                .style(EditorButton.Style.TAB).selected(this.inventoryOutput.isPresent()).build());
        EditorButton clearInventoryFilter = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.recipe_import.inventory_clear"),
                        button -> clearInventoryFilter())
                .bounds(layout.left + 12 + pickerWidth, layout.top + 82, clearWidth, 20)
                .style(EditorButton.Style.QUIET).build());
        clearInventoryFilter.active = this.inventoryOutput.isPresent();

        int actionY = layout.top + layout.height - 30;
        this.previousPage = this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            requestResultRefresh();
        }).bounds(layout.left + 8, actionY, 34, 20).style(EditorButton.Style.QUIET).build());
        this.nextPage = this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
            this.page = Math.min(maxPage(pageSize(layout)), this.page + 1);
            requestResultRefresh();
        }).bounds(layout.left + layout.listWidth - 42, actionY, 34, 20)
                .style(EditorButton.Style.QUIET).build());

        int detailX = layout.detailX;
        int detailWidth = layout.detailWidth;
        int cancelWidth = Math.min(84, detailWidth / 2 - 3);
        this.importButton = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.import_recipe"),
                        button -> importSelected())
                .bounds(detailX + cancelWidth + 5, actionY,
                        detailWidth - cancelWidth - 5, 20)
                .style(EditorButton.Style.PRIMARY).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                        button -> onClose())
                .bounds(detailX, actionY, cancelWidth, 20)
                .style(EditorButton.Style.QUIET).build());
        refreshResults();
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
        updateFilteredRecipes();
    }

    private void filter(String value) {
        this.query = value;
        updateFilteredRecipes();
        resetFilteredSelection();
        requestResultRefresh();
    }

    private void updateFilteredRecipes() {
        this.filteredRecipes.clear();
        this.allRecipes.stream()
                .filter(this::matchesFilters)
                .forEach(this.filteredRecipes::add);
    }

    private boolean matchesFilters(Recipe<?> recipe) {
        ItemStack output = result(recipe);
        ResourceLocation outputId = output.isEmpty()
                ? null
                : ForgeRegistries.ITEMS.getKey(output.getItem());
        if (this.inventoryOutput.isPresent() && !this.inventoryOutput.get().equals(outputId)) {
            return false;
        }
        return SearchText.matches(this.query, recipe.getId().toString(),
                outputId == null ? "" : outputId.toString(), output.getHoverName().getString());
    }

    private void resetFilteredSelection() {
        this.page = 0;
        this.selected = null;
        this.selectedInspection = Optional.empty();
    }

    private void openInventoryFilter() {
        Minecraft.getInstance().setScreen(RegistryItemSelectScreen.inventory(this, id -> {
            this.inventoryOutput = Optional.of(id);
            updateFilteredRecipes();
            resetFilteredSelection();
        }));
    }

    private void clearInventoryFilter() {
        this.inventoryOutput = Optional.empty();
        updateFilteredRecipes();
        resetFilteredSelection();
        this.clearWidgets();
        this.init();
    }

    private Component inventoryFilterLabel() {
        return this.inventoryOutput
                .map(id -> Component.translatable("gui.avaritia_tweak.recipe_import.inventory_selected",
                        RegistryItemSelectScreen.displayName(id)))
                .orElseGet(() -> Component.translatable(
                        "gui.avaritia_tweak.recipe_import.inventory_pick"));
    }

    private void importSelected() {
        if (this.selected == null) {
            return;
        }
        if (this.selectedInspection.isEmpty()) {
            this.selectedInspection = inspect(this.selected);
        }
        if (this.selectedInspection.isEmpty()) {
            return;
        }
        this.onImported.accept(this.selectedInspection.orElseThrow());
        Minecraft.getInstance().setScreen(this.previous);
    }

    private Optional<RecipeImportResult> inspect(Recipe<?> recipe) {
        if (Minecraft.getInstance().level == null) {
            return Optional.empty();
        }
        return Optional.of(this.importer.importRecipe(recipe, this.target,
                Minecraft.getInstance().level.registryAccess()));
    }

    private void selectRecipe(Recipe<?> recipe) {
        this.selected = recipe;
        this.selectedInspection = inspect(recipe);
        requestResultRefresh();
    }

    private ItemStack result(Recipe<?> recipe) {
        if (Minecraft.getInstance().level == null) {
            return ItemStack.EMPTY;
        }
        return recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
    }

    private void refreshResults() {
        this.resultWidgets.forEach(this::removeWidget);
        this.resultWidgets.clear();
        this.visibleRows.clear();
        Layout layout = layout();
        int pageSize = pageSize(layout);
        int maxPage = maxPage(pageSize);
        this.page = Math.min(this.page, maxPage);
        int start = this.page * pageSize;
        for (int index = start; index < Math.min(this.filteredRecipes.size(), start + pageSize); index++) {
            Recipe<?> recipe = this.filteredRecipes.get(index);
            int y = layout.listTop + (index - start) * 22;
            String id = ScreenText.fit(this.font, recipe.getId().toString(), layout.listWidth - 58);
            EditorButton row = EditorButton.builder(Component.literal(id), button -> selectRecipe(recipe))
                    .bounds(layout.left + 34, y, layout.listWidth - 42, 20)
                    .style(EditorButton.Style.LIST).selected(recipe == this.selected).build();
            this.resultWidgets.add(this.addRenderableWidget(row));
            this.visibleRows.add(new RecipeRow(recipe, layout.left + 10, y + 1));
        }
        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page < maxPage;
        this.importButton.active = this.selected != null && this.selectedInspection.isPresent();
    }

    private void requestResultRefresh() {
        this.searchRefresh.request(this, this::refreshResults);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();
        if (mouseX > layout.left + layout.listWidth) {
            return false;
        }
        int pageSize = pageSize(layout);
        int next = Math.max(0, Math.min(maxPage(pageSize),
                this.page + (delta < 0 ? 1 : -1)));
        if (next != this.page) {
            this.page = next;
            requestResultRefresh();
        }
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
        Optional<Component> inspectorTooltip = renderSelected(graphics, layout, mouseX, mouseY);

        int pageSize = pageSize(layout);
        graphics.drawCenteredString(this.font,
                (this.page + 1) + "/" + (maxPage(pageSize) + 1),
                layout.left + layout.listWidth / 2,
                layout.top + layout.height - 24, EditorTheme.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
        inspectorTooltip.ifPresent(tooltip -> graphics.renderTooltip(this.font, tooltip, mouseX, mouseY));
    }

    private void renderRecipeIcons(GuiGraphics graphics, int mouseX, int mouseY) {
        for (RecipeRow row : this.visibleRows) {
            EditorTheme.renderSlot(graphics, row.x, row.y, 20, 20,
                    mouseX >= row.x && mouseX < row.x + 20 && mouseY >= row.y && mouseY < row.y + 20);
            ItemStack stack = result(row.recipe);
            graphics.renderItem(stack, row.x + 2, row.y + 2);
        }
    }

    private Optional<Component> renderSelected(GuiGraphics graphics, Layout layout,
                                               int mouseX, int mouseY) {
        if (this.selected == null) {
            graphics.drawWordWrap(this.font,
                    Component.translatable("gui.avaritia_tweak.recipe_import.select_help"),
                    layout.detailX, layout.top + 68, layout.detailWidth, EditorTheme.TEXT_MUTED);
            return Optional.empty();
        }
        ItemStack result = result(this.selected);
        int slotX = layout.detailX;
        int slotY = layout.top + 62;
        int slotSize = Math.min(38, Math.max(24, layout.detailWidth / 4));
        int itemOffset = Math.max(1, (slotSize - 16) / 2);
        EditorTheme.renderSlot(graphics, slotX, slotY, slotSize, slotSize, false);
        graphics.renderItem(result, slotX + itemOffset, slotY + itemOffset);
        graphics.renderItemDecorations(this.font, result, slotX + itemOffset, slotY + itemOffset);
        graphics.drawString(this.font,
                ScreenText.fit(this.font, result.getHoverName().getString(),
                        Math.max(0, layout.detailWidth - slotSize - 8)),
                slotX + slotSize + 6, slotY + 4, EditorTheme.TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.recipe_import.result"),
                slotX + slotSize + 6, slotY + 17, EditorTheme.TEXT_FAINT, false);

        int y = slotY + slotSize + 6;
        y = renderInspectorLine(graphics, layout.detailX, y, layout.detailWidth,
                Component.translatable("gui.avaritia_tweak.recipe_import.id_short"),
                this.selected.getId().toString(), EditorTheme.AVARITIA_CYAN);

        RecipeImportResult inspection = this.selectedInspection.orElse(null);
        if (inspection instanceof RecipeImportResult.Failure failure) {
            return renderImportFailure(graphics, layout, y, failure);
        }
        if (!(inspection instanceof RecipeImportResult.Success success)) {
            graphics.drawWordWrap(this.font,
                    Component.translatable("gui.avaritia_tweak.recipe_import.details_unavailable"),
                    layout.detailX, y + 2, layout.detailWidth, EditorTheme.ERROR);
            return Optional.empty();
        }

        CustomizationEntry entry = success.entry();
        RecipeInspectorDetails.Details details = RecipeInspectorDetails.from(entry);
        y = renderInspectorLine(graphics, layout.detailX, y, layout.detailWidth,
                Component.translatable("gui.avaritia_tweak.recipe_import.type"),
                Component.translatable("gui.avaritia_tweak.kind."
                        + details.kind().name().toLowerCase(Locale.ROOT)).getString(), EditorTheme.TEXT);
        boolean compact = layout.detailWidth < 180 || layout.height < 300;
        if (!compact) {
            y = renderInspectorLine(graphics, layout.detailX, y, layout.detailWidth,
                    Component.translatable("gui.avaritia_tweak.recipe_import.handler"),
                    this.selected.getClass().getSimpleName(), EditorTheme.SUCCESS);
        }
        for (RecipeInspectorDetails.Attribute attribute : details.attributes()) {
            y = renderInspectorLine(graphics, layout.detailX, y, layout.detailWidth,
                    Component.translatable(attribute.labelKey()), attribute.value(), EditorTheme.TEXT);
        }
        String ingredientCount = details.cells().size() == details.slotCount()
                ? Integer.toString(details.cells().size())
                : details.cells().size() + "/" + details.slotCount();
        y = renderInspectorLine(graphics, layout.detailX, y, layout.detailWidth,
                Component.translatable("gui.avaritia_tweak.recipe_import.ingredients"),
                ingredientCount, EditorTheme.AVARITIA_GOLD);
        return renderIngredientGrid(graphics, layout, details, y + 1, mouseX, mouseY);
    }

    private Optional<Component> renderImportFailure(GuiGraphics graphics, Layout layout, int y,
                                                    RecipeImportResult.Failure failure) {
        graphics.drawString(this.font,
                Component.translatable("gui.avaritia_tweak.recipe_import.cannot_decode"),
                layout.detailX, y + 2, EditorTheme.ERROR, false);
        String detail = failure.fieldPath() + ": " + failure.message();
        graphics.enableScissor(layout.detailX, y + 13,
                layout.detailX + layout.detailWidth, layout.detailsBottom);
        graphics.drawWordWrap(this.font, Component.literal(detail), layout.detailX, y + 13,
                layout.detailWidth, EditorTheme.TEXT_MUTED);
        graphics.disableScissor();
        return Optional.empty();
    }

    private int renderInspectorLine(GuiGraphics graphics, int x, int y, int width,
                                    Component label, String value, int valueColor) {
        String labelText = label.getString() + ":";
        int valueX = x + this.font.width(labelText) + 4;
        if (valueX >= x + width - 8) {
            String line = ScreenText.fit(this.font, labelText + " " + value, width);
            graphics.drawString(this.font, line, x, y, valueColor, false);
            return y + 11;
        }
        graphics.drawString(this.font, labelText, x, y, EditorTheme.TEXT_MUTED, false);
        String fitted = ScreenText.fit(this.font, value, Math.max(0, x + width - valueX));
        graphics.drawString(this.font, fitted, valueX, y, valueColor, false);
        return y + 11;
    }

    private Optional<Component> renderIngredientGrid(GuiGraphics graphics, Layout layout,
                                                     RecipeInspectorDetails.Details details,
                                                     int gridTop, int mouseX, int mouseY) {
        int availableHeight = Math.max(1, layout.detailsBottom - gridTop - 2);
        int cellByWidth = Math.max(1, layout.detailWidth / details.columns());
        int cellByHeight = Math.max(1, availableHeight / details.rows());
        int cellSize = Math.max(4, Math.min(18, Math.min(cellByWidth, cellByHeight)));
        int gridWidth = details.columns() * cellSize;
        int gridX = layout.detailX + Math.max(0, (layout.detailWidth - gridWidth) / 2);
        Optional<Component> tooltip = Optional.empty();
        graphics.enableScissor(layout.detailX, gridTop,
                layout.detailX + layout.detailWidth, layout.detailsBottom);
        for (int slot = 0; slot < details.slotCount(); slot++) {
            int x = gridX + slot % details.columns() * cellSize;
            int y = gridTop + slot / details.columns() * cellSize;
            RecipeInspectorDetails.IngredientCell cell = details.cells().get(slot);
            boolean hovered = mouseX >= x && mouseX < x + cellSize
                    && mouseY >= y && mouseY < y + cellSize
                    && mouseY >= gridTop && mouseY < layout.detailsBottom;
            Optional<IngredientSpec> ingredient =
                    Optional.ofNullable(cell).map(RecipeInspectorDetails.IngredientCell::ingredient);
            GhostIngredientButton.renderReadOnlyPreview(graphics, x, y, cellSize, ingredient, hovered);
            if (hovered && cell != null) {
                Component description = Component.literal(GhostIngredientButton.describe(cell.ingredient()));
                tooltip = cell.roleKey().isEmpty()
                        ? Optional.of(description)
                        : Optional.of(Component.translatable(
                        "gui.avaritia_tweak.recipe_import.ingredient_role",
                        Component.translatable(cell.roleKey()), description));
            }
        }
        graphics.disableScissor();
        return tooltip;
    }

    private int pageSize(Layout layout) {
        return Math.max(1, (layout.detailsBottom - layout.listTop - 4) / 22);
    }

    private int maxPage(int pageSize) {
        return Math.max(0, (this.filteredRecipes.size() - 1) / pageSize);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 720, 390);
        int panelWidth = frame.width();
        int panelHeight = frame.height();
        int left = frame.left();
        int top = frame.top();
        EditorUiScale.SplitPane split = EditorUiScale.recipeSelectorSplit(panelWidth);
        int listWidth = split.listWidth();
        int detailX = left + listWidth + 10;
        int detailWidth = split.detailWidth();
        int listTop = top + 108;
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
