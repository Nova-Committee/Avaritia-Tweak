package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.AvaritiaTweak;
import committee.nova.mods.avaritia_tweak.client.customization.ClientCustomizationServices;
import committee.nova.mods.avaritia_tweak.client.customization.EditorController;
import committee.nova.mods.avaritia_tweak.client.customization.EntryNavigation;
import committee.nova.mods.avaritia_tweak.client.customization.EntryForm;
import committee.nova.mods.avaritia_tweak.client.customization.EntryFormException;
import committee.nova.mods.avaritia_tweak.client.customization.importers.RecipeImportResult;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorMenu;
import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.diff.EntryChange;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiff;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiffer;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.SingularityAction;
import committee.nova.mods.avaritia_tweak.customization.render.PreviewResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class CustomizationEditorScreen extends AbstractContainerScreen<RecipeGeneratorMenu> {
    private static final ResourceLocation TABLE_ICON = Objects.requireNonNull(ResourceLocation.tryBuild(
            AvaritiaTweak.MOD_ID, "textures/block/recipe_generator_table_top.png"));
    private static final int HEADER_HEIGHT = 34;
    private static final int FOOTER_HEIGHT = 20;
    private static final int PANEL_HEADER_HEIGHT = 18;
    private static final int NAVIGATION_WIDTH = 180;
    private static final int OUTPUT_WIDTH = 224;
    private static final int ENTRY_ROW_HEIGHT = 30;
    private static final int ENTRY_PAGINATION_HEIGHT = 26;
    private static final int ENTRY_FIELD_GAP = 6;
    private static final int INGREDIENT_PAGE_SIZE = 18;
    private final EditorController controller;
    private final WorkspaceDiffer differ = new WorkspaceDiffer();
    private final Map<String, EditBox> fields = new HashMap<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<ChangeIndicator> changeIndicators = new ArrayList<>();
    private final List<EditorButton> navigationResultWidgets = new ArrayList<>();
    private final List<Label> navigationResultLabels = new ArrayList<>();
    private final List<ChangeIndicator> navigationResultIndicators = new ArrayList<>();
    private final DeferredSearchRefresh navigationRefresh = new DeferredSearchRefresh();
    private final TableIngredientBrush tableBrush = new TableIngredientBrush();
    private final TableSlotDragGesture tableSlotDrag = new TableSlotDragGesture();
    private final EditorClipboard clipboard = new EditorClipboard();
    private EntryForm form;
    private Optional<EntryKey> editingKey = Optional.empty();
    private Panel activePanel;
    private boolean wideLayout;
    private int navigationX;
    private int navigationWidth;
    private int editorX;
    private int editorWidth;
    private int outputX;
    private int outputWidth;
    private int panelTop;
    private int panelBottom;
    private int entryPage;
    private int ingredientPage;
    private int navigationResultsX;
    private int navigationResultsY;
    private int navigationResultsWidth;
    private String status = "";
    private StatusTone statusTone = StatusTone.MUTED;
    private String entryQuery = "";
    private int themeButtonX;
    private int themeButtonWidth;
    private EditorUiScale.ShapedGrid tableGridLayout;
    private EntryKind tableGridKind;
    private int lastBrushedSlot = -1;
    private EditorContextMenu contextMenu;
    private Map<String, String> fieldTextOnReopen = Map.of();

    public CustomizationEditorScreen(RecipeGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        ClientCustomizationServices services = ClientCustomizationServices.get();
        this.controller = services.controller();
        services.startupFailure().ifPresent(failure -> {
            setStatus(CommitResultMessages.describe(failure), StatusTone.ERROR);
        });
        if (this.status.isEmpty()) {
            EditorTheme.startupWarning().ifPresent(warning -> setStatus(Component.translatable(
                    "gui.avaritia_tweak.theme_load_failed", warning).getString(), StatusTone.ERROR));
        }
        this.imageWidth = 1;
        this.imageHeight = 1;
        this.activePanel = Panel.from(this.controller.activePanel());
        Optional<CustomizationEntry> selected = this.controller.selectedEntry()
                .map(this.controller.draft().entries()::get);
        if (selected.isPresent()) {
            this.form = EntryForm.from(selected.get());
            this.editingKey = Optional.of(selected.get().key());
        } else {
            this.form = EntryForm.newEntry(EntryKind.SHAPED_TABLE);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.fields.clear();
        this.labels.clear();
        this.changeIndicators.clear();
        this.navigationResultWidgets.clear();
        this.navigationResultLabels.clear();
        this.navigationResultIndicators.clear();
        this.tableGridLayout = null;
        this.tableGridKind = null;
        this.lastBrushedSlot = -1;
        this.tableSlotDrag.cancel();
        this.contextMenu = null;
        this.wideLayout = EditorUiScale.isWide(this.width, this.height, 680, 300);
        if (!this.wideLayout && !this.fieldTextOnReopen.isEmpty()) {
            this.activePanel = Panel.EDITOR;
            this.controller.activePanel(this.activePanel.id);
        }
        this.panelTop = HEADER_HEIGHT + 2;
        this.panelBottom = this.height - FOOTER_HEIGHT;
        EditorUiScale.EditorHeader header = EditorUiScale.editorHeader(this.width);
        this.themeButtonWidth = header.themeWidth();
        this.themeButtonX = header.themeX();
        addThemeSwitcher();
        if (this.wideLayout) {
            this.navigationX = 8;
            this.navigationWidth = NAVIGATION_WIDTH;
            this.outputWidth = OUTPUT_WIDTH;
            this.outputX = this.width - this.outputWidth - 8;
            this.editorX = this.navigationX + this.navigationWidth + 8;
            this.editorWidth = Math.max(250, this.outputX - this.editorX - 8);
            addNavigationPanel();
            addEditorPanel();
            addOutputPanel();
        } else {
            addPanelTabs();
            this.navigationX = 8;
            this.navigationWidth = this.width - 16;
            this.editorX = 8;
            this.editorWidth = this.width - 16;
            this.outputX = 8;
            this.outputWidth = this.width - 16;
            switch (this.activePanel) {
                case NAVIGATION -> addNavigationPanel();
                case EDITOR -> addEditorPanel();
                case OUTPUT -> addOutputPanel();
            }
        }
        restoreFieldTextOnReopen();
    }

    private void addPanelTabs() {
        int width = EditorUiScale.editorHeader(this.width).tabWidth();
        for (int index = 0; index < Panel.values().length; index++) {
            Panel panel = Panel.values()[index];
            this.addRenderableWidget(EditorButton.builder(panel.label(), button -> {
                captureFields();
                this.activePanel = panel;
                this.controller.activePanel(panel.id);
                rebuild();
            }).bounds(8 + index * width, 7, Math.max(1, width - 2), 20)
                    .style(EditorButton.Style.TAB).selected(panel == this.activePanel).build());
        }
    }

    private void addThemeSwitcher() {
        Component themeName = Component.translatable(EditorTheme.currentStyle().translationKey());
        Component label = Component.translatable("gui.avaritia_tweak.theme", themeName);
        this.addRenderableWidget(EditorButton.builder(label, button -> switchTheme())
                .bounds(this.themeButtonX, 7, this.themeButtonWidth, 20)
                .style(EditorButton.Style.QUIET).build());
    }

    private void switchTheme() {
        EditorTheme.ThemeSwitchResult result = EditorTheme.cycleTheme();
        Component themeName = Component.translatable(result.style().translationKey());
        if (result.warning().isPresent()) {
            setStatus(Component.translatable("gui.avaritia_tweak.theme_save_failed",
                    themeName, result.warning().orElseThrow()).getString(), StatusTone.ERROR);
        } else {
            setStatus(Component.translatable("gui.avaritia_tweak.theme_changed",
                    themeName).getString(), StatusTone.SUCCESS);
        }
        rebuildPreservingFieldText();
    }

    private void addNavigationPanel() {
        int x = this.navigationX + 8;
        int y = this.panelTop + PANEL_HEADER_HEIGHT + 8;
        int width = this.navigationWidth - 16;
        int gap = 3;
        int actionWidth = (width - gap * 2) / 3;
        int actionY = y;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.new_entry"),
                button -> openNewEntryTypeSelector())
                .bounds(x, actionY, actionWidth, 20).style(EditorButton.Style.PRIMARY).build());
        EditorButton duplicate = this.addRenderableWidget(EditorButton.builder(
                Component.translatable("gui.avaritia_tweak.duplicate_entry"), button -> duplicateSelected())
                .bounds(x + actionWidth + gap, actionY, actionWidth, 20).style(EditorButton.Style.QUIET).build());
        duplicate.active = this.editingKey
                .map(this.controller.draft().entries()::containsKey)
                .orElse(false);
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.import_recipe"),
                button -> openRecipeImporter()).bounds(x + (actionWidth + gap) * 2, actionY,
                width - (actionWidth + gap) * 2, 20).style(EditorButton.Style.QUIET).build());
        y += 24;

        EditBox searchBox = new EditBox(this.font, x, y, width, 18,
                Component.translatable("gui.avaritia_tweak.search_entries"));
        searchBox.setMaxLength(256);
        searchBox.setValue(this.entryQuery);
        searchBox.setHint(Component.translatable("gui.avaritia_tweak.search_entries"));
        searchBox.setResponder(this::filterEntries);
        this.addRenderableWidget(searchBox);
        y += 24;
        this.navigationResultsX = x;
        this.navigationResultsY = y;
        this.navigationResultsWidth = width;
        refreshNavigationResults();
    }

    private void openNewEntryTypeSelector() {
        this.fieldTextOnReopen = snapshotFieldText();
        captureFields();
        Minecraft.getInstance().setScreen(new EntryTypeSelectScreen(this, this::startNewEntry));
    }

    private void startNewEntry(EntryKind kind) {
        this.fieldTextOnReopen = Map.of();
        this.form = EntryForm.newEntry(kind);
        this.editingKey = Optional.empty();
        this.controller.select(Optional.empty());
        this.ingredientPage = 0;
        setStatus(Component.translatable("gui.avaritia_tweak.new_entry_ready", kindLabel(kind)).getString(),
                StatusTone.MUTED);
        this.activePanel = Panel.EDITOR;
        this.controller.activePanel(this.activePanel.id);
    }

    private void refreshNavigationResults() {
        this.navigationResultWidgets.forEach(this::removeWidget);
        this.navigationResultWidgets.clear();
        this.navigationResultLabels.clear();
        this.navigationResultIndicators.clear();

        int x = this.navigationResultsX;
        int y = this.navigationResultsY;
        int width = this.navigationResultsWidth;
        List<CustomizationEntry> entries = EntryNavigation.search(
                this.controller.draft().entries().values(), this.entryQuery);
        int pageSize = EditorUiScale.listPageSize(y, this.panelBottom,
                ENTRY_ROW_HEIGHT, ENTRY_PAGINATION_HEIGHT);
        int maxPage = Math.max(0, (entries.size() - 1) / pageSize);
        this.entryPage = Math.min(this.entryPage, maxPage);
        int start = this.entryPage * pageSize;
        WorkspaceDiff workspaceDiff = this.differ.diff(this.controller.committed(), this.controller.draft());
        for (int index = start; index < Math.min(entries.size(), start + pageSize); index++) {
            CustomizationEntry entry = entries.get(index);
            boolean hasNote = !entry.note().isBlank();
            Component primary = Component.literal(hasNote ? entry.note() : entry.id().toString());
            Component secondary = Component.literal(hasNote ? entry.id().toString() : entry.kind().name());
            boolean selected = this.editingKey.filter(entry.key()::equals).isPresent();
            int rowY = y;
            addNavigationResultWidget(EditorButton.builder(primary, button -> selectEntry(entry))
                    .bounds(x, rowY, width, 26).style(EditorButton.Style.LIST).selected(selected)
                    .secondary(secondary)
                    .onSecondaryPress((button, mouseX, mouseY) ->
                            openEntryContextMenu(entry, mouseX, mouseY))
                    .build());
            workspaceDiff.entries().stream().filter(change -> change.key().equals(entry.key())).findFirst()
                    .ifPresent(change -> this.navigationResultIndicators.add(
                            new ChangeIndicator(x + width - 11, rowY + 9, change.type())));
            y += ENTRY_ROW_HEIGHT;
        }
        if (entries.isEmpty()) {
            this.navigationResultLabels.add(new Label(x, y + 4,
                    Component.translatable("gui.avaritia_tweak.no_entries"), EditorTheme.TEXT_FAINT));
        }
        if (maxPage > 0) {
            int pageY = this.panelBottom - 22;
            addNavigationResultWidget(EditorButton.builder(Component.literal("<"), button -> {
                this.entryPage = Math.max(0, this.entryPage - 1);
                requestNavigationRefresh();
            }).bounds(x, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
            this.navigationResultLabels.add(new Label(x + 38, pageY + 5,
                    Component.literal((this.entryPage + 1) + "/" + (maxPage + 1)), EditorTheme.TEXT_MUTED));
            addNavigationResultWidget(EditorButton.builder(Component.literal(">"), button -> {
                this.entryPage = Math.min(maxPage, this.entryPage + 1);
                requestNavigationRefresh();
            }).bounds(x + width - 30, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
        }
    }

    private void addNavigationResultWidget(EditorButton widget) {
        this.navigationResultWidgets.add(this.addRenderableWidget(widget));
    }

    private void requestNavigationRefresh() {
        this.navigationRefresh.request(this, this::refreshNavigationResults);
    }

    private void addEditorPanel() {
        int x = this.editorX + 10;
        int y = this.panelTop + PANEL_HEADER_HEIGHT + 8;
        int innerWidth = this.editorWidth - 20;
        boolean denseTable = this.form.kind().usesTableGrid()
                && this.panelBottom - this.panelTop < 350;
        if (denseTable) {
            addDenseTableEditor(x, y, innerWidth);
        } else {
            addEntryIdentityFields(x, y, innerWidth);
            y += 30;
            int buttonWidth = Math.min(150, innerWidth / 2);
            addTargetButton(x, y, buttonWidth);
            this.labels.add(new Label(x + buttonWidth + 8, y + 6,
                    kindLabel(this.form.kind()), EditorTheme.AVARITIA_GOLD));
            y += 28;

            switch (this.form.kind()) {
                case SHAPED_TABLE, NO_CONSUME_CATALYST_SHAPED -> addShapedEditor(x, y, innerWidth);
                case SHAPELESS_TABLE -> addShapelessEditor(x, y, innerWidth);
                case COMPRESSOR -> addCompressorEditor(x, y, innerWidth);
                case EXTREME_SMITHING -> addSmithingEditor(x, y, innerWidth);
                case INFINITY_CATALYST -> addCatalystEditor(x, y, innerWidth);
                case ETERNAL_SINGULARITY -> addEternalEditor(x, y, innerWidth);
                case SINGULARITY_DEFINITION -> addSingularityEditor(x, y, innerWidth);
                case SINGULARITY_OPERATION -> addOperationEditor(x, y, innerWidth);
            }
        }

        int actionsY = this.panelBottom - 28;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.stage"), button -> {
            if (stageForm()) {
                setStatus("Staged " + this.form.idText(), StatusTone.SUCCESS);
                if (!this.wideLayout) {
                    this.activePanel = Panel.OUTPUT;
                    this.controller.activePanel(this.activePanel.id);
                }
                rebuild();
            }
        }).bounds(x, actionsY, 92, 20).style(EditorButton.Style.PRIMARY).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.delete"),
                        button -> deleteEditingEntry())
                .bounds(x + 98, actionsY, 82, 20).style(EditorButton.Style.DANGER).build());
    }

    private void addTargetButton(int x, int y, int width) {
        this.addRenderableWidget(EditorButton.builder(targetLabel(this.form.target()), button -> {
            captureFields();
            List<OutputTarget> targets = supportedTargets(this.form.kind());
            int current = targets.indexOf(this.form.target());
            this.form.target(targets.get((current + 1) % targets.size()));
            rebuild();
        }).bounds(x, y, width, 20).style(EditorButton.Style.QUIET).build());
    }

    private void addEntryIdentityFields(int x, int y, int width) {
        EditorUiScale.EqualColumns fields = EditorUiScale.equalColumns(
                x, width, ENTRY_FIELD_GAP);
        addField("id", Component.translatable("gui.avaritia_tweak.entry_id"),
                this.form.idText(), fields.firstX(), y, fields.firstWidth());
        addField("note", Component.translatable("gui.avaritia_tweak.entry_note"),
                this.form.note(), fields.secondX(), y, fields.secondWidth());
    }

    private void addDenseTableEditor(int x, int y, int width) {
        int tierWidth = Math.min(76, Math.max(62, width / 4));
        int targetWidth = Math.min(82, Math.max(68, width / 4));
        addEntryIdentityFields(x, y, width);
        int targetX = x;
        this.labels.add(new Label(targetX, y + 34, kindLabel(this.form.kind()), EditorTheme.AVARITIA_GOLD));
        addTargetButton(targetX, y + 44, targetWidth);
        int tierX = targetX + targetWidth + 6;
        this.labels.add(new Label(tierX, y + 34,
                Component.literal(this.form.tier().gridSize() + "×" + this.form.tier().gridSize()),
                EditorTheme.TEXT_MUTED));
        addTierButton(tierX, y + 44, Math.min(tierWidth, Math.max(1, x + width - tierX)));
        addTableBrushToolbar(x, y + 68, width);
        if (this.form.kind().usesShapedGrid()) {
            addShapedGrid(x, y + 92, width);
        } else {
            addShapelessGrid(x, y + 92, width);
        }
    }

    private void addTableBrushToolbar(int x, int y, int width) {
        this.addRenderableWidget(new GhostIngredientButton(x, y + 1,
                this.tableBrush::selection, button -> openTableBrushSelector()));
        int labelWidth = Math.min(92, Math.max(48, width / 4));
        String label = ScreenText.fit(this.font,
                Component.translatable("gui.avaritia_tweak.brush.select").getString(), labelWidth - 2);
        this.labels.add(new Label(x + 24, y + 6, Component.literal(label),
                this.tableBrush.active() ? EditorTheme.AVARITIA_CYAN : EditorTheme.TEXT_MUTED));
        int actionsX = x + 24 + labelWidth;
        int available = Math.max(2, x + width - actionsX - 4);
        int fillWidth = Math.max(1, (available - 4) * 3 / 5);
        int disableWidth = Math.max(1, available - fillWidth - 4);
        EditorButton fill = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.brush.fill_empty"),
                        button -> fillTableWithBrush())
                .bounds(actionsX, y, fillWidth, 20)
                .style(EditorButton.Style.PRIMARY).build());
        fill.active = this.tableBrush.active();
        EditorButton disable = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.brush.disable"),
                        button -> disableTableBrush())
                .bounds(actionsX + fillWidth + 4, y, disableWidth, 20)
                .style(EditorButton.Style.QUIET).build());
        disable.active = this.tableBrush.active();
    }

    private void openTableBrushSelector() {
        if (!captureFields()) {
            return;
        }
        Minecraft.getInstance().setScreen(new RegistryItemSelectScreen(this, id -> {
            this.tableBrush.select(Optional.of(new IngredientSpec.Item(id)));
            setStatus(Component.translatable("gui.avaritia_tweak.brush.selected",
                    RegistryItemSelectScreen.displayName(id)).getString(), StatusTone.SUCCESS);
        }));
    }

    private void fillTableWithBrush() {
        int changed = this.tableBrush.fillEmpty(this.form);
        setStatus(Component.translatable("gui.avaritia_tweak.brush.filled", changed).getString(),
                changed > 0 ? StatusTone.SUCCESS : StatusTone.MUTED);
    }

    private void disableTableBrush() {
        this.tableBrush.clear();
        setStatus(Component.translatable("gui.avaritia_tweak.brush.disabled").getString(),
                StatusTone.MUTED);
        rebuildPreservingFieldText();
    }

    private void addShapedEditor(int x, int y, int width) {
        addTierButton(x, y);
        this.labels.add(new Label(x + 104, y + 6,
                Component.literal(this.form.tier().gridSize() + "×" + this.form.tier().gridSize()),
                EditorTheme.TEXT_MUTED));
        addTableBrushToolbar(x, y + 26, width);
        addShapedGrid(x, y + 50, width);
    }

    private void addShapedGrid(int x, int y, int width) {
        int size = this.form.tier().gridSize();
        int availableHeight = this.panelBottom - 32 - y;
        EditorUiScale.ShapedGrid layout = EditorUiScale.shapedGrid(
                x, y, width, availableHeight, size);
        this.tableGridLayout = layout;
        this.tableGridKind = this.form.kind();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int slot = row * size + column;
                this.addRenderableWidget(new GhostIngredientButton(
                        layout.gridX() + column * layout.cellSize(),
                        layout.gridY() + row * layout.cellSize(), layout.cellSize(),
                        () -> Optional.ofNullable(this.form.grid().get(slot)),
                        button -> handleShapedSlot(slot),
                        (button, mouseX, mouseY) -> openTableIngredientContextMenu(
                                slot, false, mouseX, mouseY)));
            }
        }
        addTableResult(layout);
    }

    private void handleShapedSlot(int slot) {
        if (this.tableBrush.active()) {
            applyTableBrush(slot);
            return;
        }
        openIngredient(Optional.ofNullable(this.form.grid().get(slot)),
                value -> this.form.gridIngredient(slot, value));
    }

    private void addShapelessEditor(int x, int y, int width) {
        addTierButton(x, y);
        this.labels.add(new Label(x + 104, y + 6,
                Component.literal(this.form.tier().gridSize() + "×" + this.form.tier().gridSize()),
                EditorTheme.TEXT_MUTED));
        addTableBrushToolbar(x, y + 26, width);
        addShapelessGrid(x, y + 50, width);
    }

    private void addShapelessGrid(int x, int y, int width) {
        int size = this.form.tier().gridSize();
        int availableHeight = this.panelBottom - 32 - y;
        EditorUiScale.ShapedGrid layout = EditorUiScale.shapedGrid(
                x, y, width, availableHeight, size);
        this.tableGridLayout = layout;
        this.tableGridKind = EntryKind.SHAPELESS_TABLE;
        int capacity = this.form.tier().capacity();
        for (int index = 0; index < capacity; index++) {
            int ingredientIndex = index;
            int row = index / size;
            int column = index % size;
            GhostIngredientButton slot = new GhostIngredientButton(
                    layout.gridX() + column * layout.cellSize(),
                    layout.gridY() + row * layout.cellSize(), layout.cellSize(),
                    () -> ingredientIndex < this.form.ingredients().size()
                            ? Optional.of(this.form.ingredients().get(ingredientIndex))
                            : Optional.empty(),
                    button -> handleShapelessSlot(ingredientIndex),
                    (button, mouseX, mouseY) -> openTableIngredientContextMenu(
                            ingredientIndex, true, mouseX, mouseY));
            slot.active = this.tableBrush.active()
                    || ingredientIndex <= this.form.ingredients().size();
            this.addRenderableWidget(slot);
        }
        addTableResult(layout);
    }

    private void openShapelessIngredient(int index) {
        List<IngredientSpec> ingredients = this.form.ingredients();
        Optional<IngredientSpec> current = index < ingredients.size()
                ? Optional.of(ingredients.get(index)) : Optional.empty();
        openIngredient(current, value -> {
            if (index < this.form.ingredients().size()) {
                if (value.isPresent()) {
                    this.form.ingredientAt(index, value.get());
                } else {
                    this.form.removeIngredient(index);
                }
            } else {
                value.ifPresent(this.form::addIngredient);
            }
        });
    }

    private void handleShapelessSlot(int index) {
        if (this.tableBrush.active()) {
            applyTableBrush(index);
            return;
        }
        openShapelessIngredient(index);
    }

    private boolean applyTableBrush(int slot) {
        this.lastBrushedSlot = slot;
        return this.tableBrush.apply(this.form, slot);
    }

    private void openTableIngredientContextMenu(int slot, boolean shapeless,
                                                double mouseX, double mouseY) {
        Optional<IngredientSpec> current = tableIngredientAt(slot, shapeless);
        List<EditorContextMenu.Item> items = new ArrayList<>();
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.edit_ingredient"),
                () -> editTableIngredient(slot, shapeless)));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.copy_ingredient"),
                () -> copyIngredient(current.orElseThrow()), current.isPresent()));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.paste_ingredient"),
                () -> pasteTableIngredient(slot, shapeless), this.clipboard.ingredient().isPresent()));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.apply_brush"), () -> {
                    boolean changed = applyTableBrush(slot);
                    setStatus(Component.translatable(
                            "gui.avaritia_tweak.context.brush_applied").getString(),
                            changed ? StatusTone.SUCCESS : StatusTone.MUTED);
                }, this.tableBrush.active()));
        if (shapeless) {
            items.add(EditorContextMenu.action(
                    Component.translatable("gui.avaritia_tweak.context.replace_matching"),
                    () -> replaceMatchingInputs(current), canReplaceMatching(current)));
        } else {
            items.add(EditorContextMenu.separatedAction(
                    Component.translatable("gui.avaritia_tweak.context.grid_operations"),
                    () -> openTableGridContextMenu(slot, current, mouseX, mouseY)));
        }
        current.ifPresent(ingredient -> {
            items.add(EditorContextMenu.separatedAction(
                    Component.translatable("gui.avaritia_tweak.context.set_brush"), () -> {
                        this.tableBrush.select(Optional.of(ingredient));
                        setStatus(Component.translatable("gui.avaritia_tweak.brush.selected",
                                GhostIngredientButton.describe(ingredient)).getString(), StatusTone.SUCCESS);
                        rebuildPreservingFieldText();
                    }));
            items.add(EditorContextMenu.danger(
                    Component.translatable("gui.avaritia_tweak.context.clear_ingredient"), () -> {
                        clearTableIngredient(slot, shapeless);
                        setStatus(Component.translatable(
                                "gui.avaritia_tweak.context.ingredient_cleared").getString(),
                                StatusTone.WARNING);
                    }));
        });
        openContextMenu(mouseX, mouseY, items);
    }

    private void openTableGridContextMenu(int slot, Optional<IngredientSpec> current,
                                          double mouseX, double mouseY) {
        List<EditorContextMenu.Item> items = new ArrayList<>();
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.fill_row"),
                () -> reportGridChange(TableGridOperations.fillRow(
                        this.form, slot, current.orElseThrow())), current.isPresent()));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.fill_column"),
                () -> reportGridChange(TableGridOperations.fillColumn(
                        this.form, slot, current.orElseThrow())), current.isPresent()));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.clear_row"),
                () -> reportGridChange(TableGridOperations.clearRow(this.form, slot))));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.clear_column"),
                () -> reportGridChange(TableGridOperations.clearColumn(this.form, slot))));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.replace_matching"),
                () -> replaceMatchingInputs(current), canReplaceMatching(current)));
        items.add(EditorContextMenu.separatedAction(
                Component.translatable("gui.avaritia_tweak.context.mirror_horizontal"),
                () -> reportGridChange(TableGridOperations.mirrorHorizontal(this.form))));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.mirror_vertical"),
                () -> reportGridChange(TableGridOperations.mirrorVertical(this.form))));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.rotate_clockwise"),
                () -> reportGridChange(TableGridOperations.rotateClockwise(this.form))));
        items.add(EditorContextMenu.separatedAction(
                Component.translatable("gui.avaritia_tweak.context.back"),
                () -> openTableIngredientContextMenu(slot, false, mouseX, mouseY)));
        openContextMenu(mouseX, mouseY, items);
    }

    private boolean canReplaceMatching(Optional<IngredientSpec> current) {
        return current.isPresent() && this.clipboard.ingredient().isPresent()
                && !current.orElseThrow().equals(this.clipboard.ingredient().orElseThrow());
    }

    private void replaceMatchingInputs(Optional<IngredientSpec> current) {
        if (current.isEmpty() || this.clipboard.ingredient().isEmpty()) {
            return;
        }
        reportGridChange(TableGridOperations.replaceAll(this.form, current.orElseThrow(),
                this.clipboard.ingredient().orElseThrow()));
    }

    private void reportGridChange(int changed) {
        Component message = changed > 0
                ? Component.translatable("gui.avaritia_tweak.context.grid_changed", changed)
                : Component.translatable("gui.avaritia_tweak.context.grid_unchanged");
        setStatus(message.getString(),
                changed > 0 ? StatusTone.SUCCESS : StatusTone.MUTED);
    }

    private void copyIngredient(IngredientSpec ingredient) {
        this.clipboard.copyIngredient(ingredient);
        setStatus(Component.translatable("gui.avaritia_tweak.context.ingredient_copied").getString(),
                StatusTone.SUCCESS);
    }

    private void pasteTableIngredient(int slot, boolean shapeless) {
        this.clipboard.ingredient().ifPresent(ingredient -> {
            setTableIngredient(slot, shapeless, Optional.of(ingredient));
            setStatus(Component.translatable("gui.avaritia_tweak.context.ingredient_pasted").getString(),
                    StatusTone.SUCCESS);
        });
    }

    private Optional<IngredientSpec> tableIngredientAt(int slot, boolean shapeless) {
        if (!shapeless) {
            return Optional.ofNullable(this.form.grid().get(slot));
        }
        return slot < this.form.ingredients().size()
                ? Optional.of(this.form.ingredients().get(slot)) : Optional.empty();
    }

    private void editTableIngredient(int slot, boolean shapeless) {
        if (shapeless) {
            openShapelessIngredient(slot);
            return;
        }
        openIngredient(Optional.ofNullable(this.form.grid().get(slot)),
                value -> this.form.gridIngredient(slot, value));
    }

    private void clearTableIngredient(int slot, boolean shapeless) {
        setTableIngredient(slot, shapeless, Optional.empty());
    }

    private void setTableIngredient(int slot, boolean shapeless, Optional<IngredientSpec> value) {
        if (shapeless) {
            if (slot < this.form.ingredients().size()) {
                if (value.isPresent()) {
                    this.form.ingredientAt(slot, value.orElseThrow());
                } else {
                    this.form.removeIngredient(slot);
                }
            } else {
                value.ifPresent(this.form::addIngredient);
            }
            return;
        }
        this.form.gridIngredient(slot, value);
    }

    private void addTableResult(EditorUiScale.ShapedGrid layout) {
        this.labels.add(new Label(layout.resultX(), layout.resultY() - 10,
                Component.translatable("gui.avaritia_tweak.result"), EditorTheme.TEXT_MUTED));
        this.addRenderableWidget(new GhostItemStackButton(layout.resultX(), layout.resultY(),
                this.form::result, button -> openResult(),
                (button, mouseX, mouseY) -> openResultContextMenu(mouseX, mouseY)));
    }

    private void addCompressorEditor(int x, int y, int width) {
        addIngredientSlot(x, y, Component.translatable("gui.avaritia_tweak.input"),
                this.form.ingredient(), this.form::ingredient);
        this.addRenderableWidget(new GhostItemStackButton(x + 128, y,
                this.form::result, button -> openResult(),
                (button, mouseX, mouseY) -> openResultContextMenu(mouseX, mouseY)));
        this.labels.add(new Label(x + 152, y + 6, Component.translatable("gui.avaritia_tweak.result"),
                EditorTheme.TEXT_MUTED));
        addField("inputCount", Component.translatable("gui.avaritia_tweak.input_count"),
                Integer.toString(this.form.inputCount()), x, y + 36, 120);
        addField("timeCost", Component.translatable("gui.avaritia_tweak.time_cost"),
                Integer.toString(this.form.timeCost()), x + 138, y + 36, 120);
    }

    private void addSmithingEditor(int x, int y, int width) {
        int spacing = Math.max(48, (width - 18) / 3);
        addCompactIngredientSlot(x, y, Component.translatable("gui.avaritia_tweak.template"),
                this.form.template(), this.form::template);
        addCompactIngredientSlot(x + spacing, y, Component.translatable("gui.avaritia_tweak.base"),
                this.form.base(), this.form::base);
        this.labels.add(new Label(x + spacing * 2, y,
                Component.translatable("gui.avaritia_tweak.result"), EditorTheme.TEXT_MUTED));
        this.addRenderableWidget(new GhostItemStackButton(x + spacing * 2, y + 11,
                this.form::result, button -> openResult(),
                (button, mouseX, mouseY) -> openResultContextMenu(mouseX, mouseY)));
        List<IngredientSpec> additions = this.form.smithingAdditions();
        for (int index = 0; index < additions.size(); index++) {
            int slot = index;
            IngredientSpec addition = additions.get(index);
            addCompactIngredientSlot(x + spacing * index, y + 42,
                    Component.translatable("gui.avaritia_tweak.addition_" + (index + 1)),
                    addition, value -> this.form.smithingAddition(slot, value));
        }
    }

    private void addCatalystEditor(int x, int y, int width) {
        addField("group", Component.translatable("gui.avaritia_tweak.group"),
                this.form.group(), x, y, 150);
        addField("count", Component.translatable("gui.avaritia_tweak.count"),
                Integer.toString(this.form.count()), x + 168, y, 90);
        addIngredientList(x, y + 34, width, 81);
    }

    private void addEternalEditor(int x, int y, int width) {
        addField("count", Component.translatable("gui.avaritia_tweak.count"),
                Integer.toString(this.form.count()), x, y, 100);
        addIngredientList(x, y + 34, width, 81);
    }

    private void addSingularityEditor(int x, int y, int width) {
        addField("displayName", Component.translatable("gui.avaritia_tweak.display_name"),
                this.form.displayName(), x, y, Math.min(240, width));
        int colorGap = 8;
        int colorWidth = Math.max(1, (width - colorGap) / 2);
        this.addRenderableWidget(EditorButton.builder(colorLabel("overlay_color", this.form.overlayColor()),
                        button -> openColorPicker(true))
                .bounds(x, y + 34, colorWidth, 20).style(EditorButton.Style.QUIET)
                .swatch(this.form.overlayColor()).build());
        this.addRenderableWidget(EditorButton.builder(colorLabel("underlay_color", this.form.underlayColor()),
                        button -> openColorPicker(false))
                .bounds(x + colorWidth + colorGap, y + 34, Math.max(1, width - colorWidth - colorGap), 20)
                .style(EditorButton.Style.QUIET).swatch(this.form.underlayColor()).build());
        int ingredientSpace = 44;
        int numberGap = 10;
        int numberWidth = Math.max(44, Math.min(90,
                (width - ingredientSpace - numberGap * 2) / 2));
        addCompactIngredientSlot(x, y + 64, Component.translatable("gui.avaritia_tweak.input"),
                this.form.ingredient(), this.form::ingredient);
        int countX = x + ingredientSpace;
        addField("count", Component.translatable("gui.avaritia_tweak.count"),
                Integer.toString(this.form.count()), countX, y + 64, numberWidth);
        addField("timeCost", Component.translatable("gui.avaritia_tweak.time_cost"),
                Integer.toString(this.form.timeCost()), countX + numberWidth + numberGap,
                y + 64, numberWidth);
        this.addRenderableWidget(EditorButton.builder(toggleLabel("enabled", this.form.enabled()), button -> {
            captureFields();
            this.form.enabled(!this.form.enabled());
            rebuild();
        }).bounds(x, y + 98, 120, 20).style(EditorButton.Style.QUIET)
                .selected(this.form.enabled()).build());
        this.addRenderableWidget(EditorButton.builder(toggleLabel("recipe", this.form.recipeEnabled()), button -> {
            captureFields();
            this.form.recipeEnabled(!this.form.recipeEnabled());
            rebuild();
        }).bounds(x + 132, y + 98, 120, 20).style(EditorButton.Style.QUIET)
                .selected(this.form.recipeEnabled()).build());
    }

    private void addOperationEditor(int x, int y, int width) {
        this.addRenderableWidget(EditorButton.builder(Component.literal(this.form.action().name()), button -> {
            captureFields();
            this.form.action(cycle(SingularityAction.values(), this.form.action()));
            rebuild();
        }).bounds(x, y, Math.min(190, width), 20).style(EditorButton.Style.DEFAULT).build());
        if (this.form.action().requiresTarget()) {
            int selectWidth = Math.min(112, Math.max(78, width / 3));
            int fieldWidth = Math.max(72, Math.min(260, width - selectWidth - 6));
            addField("singularityId", Component.translatable("gui.avaritia_tweak.singularity_id"),
                    this.form.singularityIdText(), x, y + 34, fieldWidth);
            this.addRenderableWidget(EditorButton.builder(
                            Component.translatable("gui.avaritia_tweak.select_singularity"),
                            button -> openSingularitySelector())
                    .bounds(x + fieldWidth + 6, y + 44,
                            Math.max(1, width - fieldWidth - 6), 18)
                    .style(EditorButton.Style.QUIET).build());
        } else {
            this.labels.add(new Label(x, y + 36,
                    Component.translatable("gui.avaritia_tweak.global_operation"), EditorTheme.AVARITIA_GOLD));
        }
    }

    private void addOutputPanel() {
        int x = this.outputX + 10;
        int y = this.panelTop + PANEL_HEADER_HEIGHT + 8;
        int width = this.outputWidth - 20;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.preview"), button -> {
            PreviewResult preview = this.controller.preview();
            if (preview.renderPlan().isPresent()) {
                Minecraft.getInstance().setScreen(new PreviewScreen(this, this.controller, preview));
            } else {
                String message = preview.validation().errors().isEmpty()
                        ? "Preview unavailable"
                        : preview.validation().errors().get(0).fieldPath() + ": "
                        + preview.validation().errors().get(0).messageKey();
                setStatus(message, StatusTone.ERROR);
            }
        }).bounds(x, y, width, 20).style(EditorButton.Style.PRIMARY).build());
        y += 26;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.history"),
                button -> Minecraft.getInstance().setScreen(new HistoryScreen(this, this.controller)))
                .bounds(x, y, width, 20).style(EditorButton.Style.DEFAULT).build());

        PreviewResult summary = this.controller.lastPreview().orElseGet(this.controller::preview);
        WorkspaceDiff workspaceDiff = summary.workspaceDiff();
        y += 32;
        this.labels.add(new Label(x, y, Component.translatable("gui.avaritia_tweak.workspace_base",
                this.controller.baseVersion()), EditorTheme.AVARITIA_GOLD));
        y += 14;
        this.labels.add(new Label(x, y, Component.literal("+" + workspaceDiff.count(ChangeType.ADDED)
                + "   ~" + workspaceDiff.count(ChangeType.MODIFIED)
                + "   -" + workspaceDiff.count(ChangeType.REMOVED)), EditorTheme.TEXT_MUTED));
        y += 14;
        this.labels.add(new Label(x, y, Component.translatable("gui.avaritia_tweak.validation_summary",
                summary.validation().errors().size(), summary.validation().warnings().size()),
                summary.validation().isValid() ? EditorTheme.SUCCESS : EditorTheme.ERROR));
        y += 19;
        int availableChangeRows = Math.max(0, Math.min(6, (this.panelBottom - y - 62) / 12));
        for (int index = 0; index < Math.min(availableChangeRows, workspaceDiff.entries().size()); index++) {
            EntryChange change = workspaceDiff.entries().get(index);
            String line = EditorTheme.changeMark(change.type()) + "  " + change.key().id();
            this.labels.add(new Label(x, y, Component.literal(ScreenText.ellipsize(line, 31)),
                    EditorTheme.changeColor(change.type())));
            y += 12;
        }
        if (workspaceDiff.entries().size() > availableChangeRows && availableChangeRows > 0) {
            this.labels.add(new Label(x, y, Component.translatable("gui.avaritia_tweak.more_changes",
                    workspaceDiff.entries().size() - availableChangeRows), EditorTheme.TEXT_FAINT));
        }

        int discardY = this.panelBottom - 52;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.discard_changes"),
                button -> {
                    this.controller.discardChanges();
                    this.form = EntryForm.newEntry(this.form.kind());
                    this.editingKey = Optional.empty();
                    setStatus("Draft reset to committed version", StatusTone.WARNING);
                    rebuild();
                }).bounds(x, discardY, width, 20).style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.clear_workspace"),
                button -> {
                    this.controller.clearDraft();
                    this.form = EntryForm.newEntry(this.form.kind());
                    this.editingKey = Optional.empty();
                    setStatus("All entries removed from draft", StatusTone.WARNING);
                    rebuild();
                }).bounds(x, discardY + 24, width, 20).style(EditorButton.Style.DANGER).build());

        int warningY = Math.min(discardY - 14, y + 14);
        this.controller.draftWarning().ifPresent(warning -> this.labels.add(new Label(x,
                warningY, Component.literal(ScreenText.ellipsize(warning, 34)),
                EditorTheme.WARNING)));
    }

    private void addTierButton(int x, int y) {
        addTierButton(x, y, 96);
    }

    private void addTierButton(int x, int y, int width) {
        this.addRenderableWidget(EditorButton.builder(Component.literal("Tier " + this.form.tier().value()), button -> {
            captureFields();
            this.form.tier(cycle(CraftingTier.values(), this.form.tier()));
            rebuild();
        }).bounds(x, y, width, 20).style(EditorButton.Style.QUIET).build());
    }

    private void addIngredientList(int x, int y, int width, int maximum) {
        List<IngredientSpec> values = this.form.ingredients();
        int maxPage = Math.max(0, (values.size() - 1) / INGREDIENT_PAGE_SIZE);
        this.ingredientPage = Math.min(this.ingredientPage, maxPage);
        int start = this.ingredientPage * INGREDIENT_PAGE_SIZE;
        int limit = Math.min(values.size(), start + INGREDIENT_PAGE_SIZE);
        for (int index = start; index < limit; index++) {
            int local = index - start;
            int slotX = x + (local % 9) * 22;
            int slotY = y + (local / 9) * 24;
            int ingredientIndex = index;
            this.addRenderableWidget(new GhostIngredientButton(slotX, slotY,
                    () -> Optional.of(this.form.ingredients().get(ingredientIndex)),
                    button -> openIngredient(Optional.of(this.form.ingredients().get(ingredientIndex)), value -> {
                        if (value.isPresent()) {
                            this.form.ingredientAt(ingredientIndex, value.get());
                        } else {
                            this.form.removeIngredient(ingredientIndex);
                        }
                    }), (button, mouseX, mouseY) -> openIngredientContextMenu(
                            Optional.of(this.form.ingredients().get(ingredientIndex)), value -> {
                                if (value.isPresent()) {
                                    this.form.ingredientAt(ingredientIndex, value.orElseThrow());
                                } else {
                                    this.form.removeIngredient(ingredientIndex);
                                }
                            }, true, mouseX, mouseY)));
        }
        if (values.size() < maximum && limit - start < INGREDIENT_PAGE_SIZE) {
            int local = limit - start;
            this.addRenderableWidget(new GhostIngredientButton(x + (local % 9) * 22,
                    y + (local / 9) * 24, Optional::empty,
                    button -> openIngredient(Optional.empty(), value -> value.ifPresent(this.form::addIngredient)),
                    (button, mouseX, mouseY) -> openIngredientContextMenu(Optional.empty(),
                            value -> value.ifPresent(this.form::addIngredient), false, mouseX, mouseY)));
        }
        this.labels.add(new Label(x, y + 53,
                Component.literal(values.size() + "/" + maximum + " ingredients"), EditorTheme.TEXT_MUTED));
        if (maxPage > 0 || values.size() >= INGREDIENT_PAGE_SIZE) {
            int pageY = y + 66;
            this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
                this.ingredientPage = Math.max(0, this.ingredientPage - 1);
                rebuild();
            }).bounds(x, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
            this.labels.add(new Label(x + 38, pageY + 5,
                    Component.literal((this.ingredientPage + 1) + "/" + (maxPage + 1)), EditorTheme.TEXT_MUTED));
            this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
                this.ingredientPage = Math.min(maxPage, this.ingredientPage + 1);
                rebuild();
            }).bounds(x + 90, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
        }
    }

    private void addIngredientSlot(int x, int y, Component label, IngredientSpec current,
                                   Consumer<IngredientSpec> setter) {
        this.addRenderableWidget(new GhostIngredientButton(x, y, () -> Optional.of(current),
                button -> openIngredient(Optional.of(current),
                        value -> value.ifPresent(setter)),
                (button, mouseX, mouseY) -> openIngredientContextMenu(Optional.of(current),
                        value -> value.ifPresent(setter), false, mouseX, mouseY)));
        this.labels.add(new Label(x + 24, y + 6, label, EditorTheme.TEXT_MUTED));
    }

    private void addCompactIngredientSlot(int x, int y, Component label, IngredientSpec current,
                                          Consumer<IngredientSpec> setter) {
        this.labels.add(new Label(x, y, label, EditorTheme.TEXT_MUTED));
        this.addRenderableWidget(new GhostIngredientButton(x, y + 11, () -> Optional.of(current),
                button -> openIngredient(Optional.of(current), value -> value.ifPresent(setter)),
                (button, mouseX, mouseY) -> openIngredientContextMenu(Optional.of(current),
                        value -> value.ifPresent(setter), false, mouseX, mouseY)));
    }

    private void addField(String key, Component label, String value, int x, int y, int width) {
        this.labels.add(new Label(x, y, label, EditorTheme.TEXT_MUTED));
        EditBox box = new EditBox(this.font, x, y + 10, width, 18, label);
        box.setMaxLength(4096);
        box.setValue(value);
        this.fields.put(key, box);
        this.addRenderableWidget(box);
    }

    private boolean captureFields() {
        try {
            if (this.fields.containsKey("id")) {
                this.form.idText(this.fields.get("id").getValue().strip());
            }
            if (this.fields.containsKey("note")) {
                this.form.note(this.fields.get("note").getValue());
            }
            if (this.fields.containsKey("inputCount")) {
                this.form.inputCount(parseInt("inputCount"));
            }
            if (this.fields.containsKey("timeCost")) {
                this.form.timeCost(parseInt("timeCost"));
            }
            if (this.fields.containsKey("count")) {
                this.form.count(parseInt("count"));
            }
            if (this.fields.containsKey("group")) {
                this.form.group(this.fields.get("group").getValue());
            }
            if (this.fields.containsKey("displayName")) {
                this.form.displayName(this.fields.get("displayName").getValue());
            }
            if (this.fields.containsKey("singularityId")) {
                this.form.singularityIdText(this.fields.get("singularityId").getValue().strip());
            }
            return true;
        } catch (NumberFormatException exception) {
            setStatus("Invalid number: " + exception.getMessage(), StatusTone.ERROR);
            return false;
        }
    }

    private boolean stageForm() {
        if (!captureFields()) {
            return false;
        }
        try {
            CustomizationEntry entry = this.form.build();
            this.controller.stage(entry, this.editingKey);
            this.editingKey = Optional.of(entry.key());
            return true;
        } catch (EntryFormException exception) {
            setStatus(exception.fieldPath() + ": " + exception.getMessage(), StatusTone.ERROR);
            return false;
        }
    }

    private int parseInt(String key) {
        return Integer.parseInt(this.fields.get(key).getValue().strip());
    }

    private void openIngredient(Optional<IngredientSpec> initial,
                                Consumer<Optional<IngredientSpec>> setter) {
        captureFields();
        Minecraft.getInstance().setScreen(new IngredientEditorScreen(this, initial, value -> {
            setter.accept(value);
            setStatus("Ghost ingredient updated locally", StatusTone.MUTED);
        }));
    }

    private void openResult() {
        captureFields();
        Minecraft.getInstance().setScreen(new ItemStackEditorScreen(this, this.form.result(), result -> {
            this.form.result(result);
            setStatus("Ghost result updated locally", StatusTone.MUTED);
        }));
    }

    private void openColorPicker(boolean overlay) {
        if (!captureFields()) {
            return;
        }
        int initial = overlay ? this.form.overlayColor() : this.form.underlayColor();
        Component title = Component.translatable("gui.avaritia_tweak."
                + (overlay ? "overlay_color" : "underlay_color"));
        Minecraft.getInstance().setScreen(new ColorPickerScreen(this, title, initial, color -> {
            if (overlay) {
                this.form.overlayColor(color);
            } else {
                this.form.underlayColor(color);
            }
            setStatus(Component.translatable("gui.avaritia_tweak.color_picker.updated",
                    String.format(java.util.Locale.ROOT, "#%06X", color)).getString(), StatusTone.SUCCESS);
        }));
    }

    private void openSingularitySelector() {
        if (!captureFields()) {
            return;
        }
        Minecraft.getInstance().setScreen(new SingularitySelectScreen(this, this.controller.draft(), id -> {
            this.form.singularityIdText(id.toString());
            setStatus(Component.translatable("gui.avaritia_tweak.singularity_selected", id).getString(),
                    StatusTone.SUCCESS);
        }));
    }

    private void openRecipeImporter() {
        captureFields();
        Minecraft.getInstance().setScreen(new RecipeSelectScreen(this, this.form.target(), result -> {
            if (result instanceof RecipeImportResult.Success success) {
                this.form = EntryForm.from(success.entry());
                this.editingKey = Optional.of(success.entry().key());
                setStatus("Recipe imported into local draft form", StatusTone.SUCCESS);
            } else {
                RecipeImportResult.Failure failure = (RecipeImportResult.Failure) result;
                setStatus(failure.fieldPath() + ": " + failure.message(), StatusTone.ERROR);
            }
        }));
    }

    private void openIngredientContextMenu(Optional<IngredientSpec> current,
                                           Consumer<Optional<IngredientSpec>> setter,
                                           boolean clearable,
                                           double mouseX, double mouseY) {
        List<EditorContextMenu.Item> items = new ArrayList<>();
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.edit_ingredient"),
                () -> openIngredient(current, setter)));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.copy_ingredient"),
                () -> copyIngredient(current.orElseThrow()), current.isPresent()));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.paste_ingredient"), () -> {
                    this.clipboard.ingredient().ifPresent(ingredient -> {
                        setter.accept(Optional.of(ingredient));
                        setStatus(Component.translatable(
                                "gui.avaritia_tweak.context.ingredient_pasted").getString(),
                                StatusTone.SUCCESS);
                        rebuildPreservingFieldText();
                    });
                }, this.clipboard.ingredient().isPresent()));
        if (clearable && current.isPresent()) {
            items.add(EditorContextMenu.danger(
                    Component.translatable("gui.avaritia_tweak.context.clear_ingredient"), () -> {
                        setter.accept(Optional.empty());
                        setStatus(Component.translatable(
                                "gui.avaritia_tweak.context.ingredient_cleared").getString(),
                                StatusTone.WARNING);
                        rebuildPreservingFieldText();
                    }));
        }
        openContextMenu(mouseX, mouseY, items);
    }

    private void openResultContextMenu(double mouseX, double mouseY) {
        ItemStackSpec current = this.form.result();
        openContextMenu(mouseX, mouseY, List.of(
                EditorContextMenu.action(
                        Component.translatable("gui.avaritia_tweak.context.edit_result"),
                        this::openResult),
                EditorContextMenu.action(
                        Component.translatable("gui.avaritia_tweak.context.copy_result"), () -> {
                            this.clipboard.copyItemStack(current);
                            setStatus(Component.translatable(
                                    "gui.avaritia_tweak.context.result_copied").getString(),
                                    StatusTone.SUCCESS);
                        }),
                EditorContextMenu.action(
                        Component.translatable("gui.avaritia_tweak.context.paste_result"), () -> {
                            this.clipboard.itemStack().ifPresent(this.form::result);
                            setStatus(Component.translatable(
                                    "gui.avaritia_tweak.context.result_pasted").getString(),
                                    StatusTone.SUCCESS);
                        }, this.clipboard.itemStack().isPresent())));
    }

    private void openEntryContextMenu(CustomizationEntry entry, double mouseX, double mouseY) {
        Optional<EntryChange> change = entryChange(entry.key());
        List<EditorContextMenu.Item> items = new ArrayList<>();
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.context.open_entry"),
                () -> selectEntry(entry)));
        items.add(EditorContextMenu.action(
                Component.translatable("gui.avaritia_tweak.duplicate_entry"),
                () -> duplicateEntry(entry)));
        items.add(EditorContextMenu.separatedAction(
                Component.translatable("gui.avaritia_tweak.context.compare_committed"),
                () -> openEntryComparison(change.orElseThrow()), change.isPresent()));
        Component restoreLabel = change.map(EntryChange::type)
                .filter(ChangeType.ADDED::equals).isPresent()
                ? Component.translatable("gui.avaritia_tweak.context.discard_added_entry")
                : Component.translatable("gui.avaritia_tweak.context.restore_committed");
        items.add(EditorContextMenu.action(restoreLabel,
                () -> restoreEntry(entry), change.isPresent()));
        items.add(EditorContextMenu.danger(
                Component.translatable("gui.avaritia_tweak.delete"),
                () -> deleteEntry(entry)));
        openContextMenu(mouseX, mouseY, items);
    }

    private Optional<EntryChange> entryChange(EntryKey key) {
        return this.differ.diff(this.controller.committed(), this.controller.draft()).entries().stream()
                .filter(change -> change.key().equals(key))
                .findFirst();
    }

    private void openEntryComparison(EntryChange change) {
        if (!captureFields()) {
            return;
        }
        Minecraft.getInstance().setScreen(new EntryCompareScreen(this, change));
    }

    private void restoreEntry(CustomizationEntry entry) {
        boolean editing = this.editingKey.filter(entry.key()::equals).isPresent();
        Map<String, String> fieldText = editing ? Map.of() : snapshotFieldText();
        if (!this.controller.restoreEntry(entry.key())) {
            return;
        }
        if (editing) {
            Optional<CustomizationEntry> restored = Optional.ofNullable(
                    this.controller.draft().entries().get(entry.key()));
            this.form = restored.map(EntryForm::from)
                    .orElseGet(() -> EntryForm.newEntry(entry.kind()));
            this.editingKey = restored.map(CustomizationEntry::key);
            this.ingredientPage = 0;
        }
        setStatus(Component.translatable("gui.avaritia_tweak.context.entry_restored").getString(),
                StatusTone.SUCCESS);
        rebuild();
        if (!editing) {
            restoreFieldText(fieldText);
        }
    }

    private void openContextMenu(double mouseX, double mouseY,
                                 List<EditorContextMenu.Item> items) {
        this.contextMenu = EditorContextMenu.open(this.font, this.width, this.height,
                mouseX, mouseY, items);
    }

    private void selectEntry(CustomizationEntry entry) {
        captureFields();
        this.form = EntryForm.from(entry);
        this.editingKey = Optional.of(entry.key());
        this.controller.select(this.editingKey);
        this.ingredientPage = 0;
        if (!this.wideLayout) {
            this.activePanel = Panel.EDITOR;
            this.controller.activePanel(this.activePanel.id);
        }
        rebuild();
    }

    private void duplicateSelected() {
        Optional<CustomizationEntry> source = this.editingKey
                .map(this.controller.draft().entries()::get);
        if (source.isEmpty()) {
            return;
        }
        duplicateEntry(source.orElseThrow());
    }

    private void duplicateEntry(CustomizationEntry source) {
        this.form = EntryNavigation.duplicateForEditing(source,
                this.controller.draft().entries().values());
        this.editingKey = Optional.empty();
        this.ingredientPage = 0;
        setStatus(Component.translatable("gui.avaritia_tweak.duplicate_ready", this.form.idText()).getString(),
                StatusTone.SUCCESS);
        if (!this.wideLayout) {
            this.activePanel = Panel.EDITOR;
            this.controller.activePanel(this.activePanel.id);
        }
        rebuild();
    }

    private void deleteEditingEntry() {
        this.editingKey.map(this.controller.draft().entries()::get)
                .ifPresentOrElse(this::deleteEntry, () -> {
                    this.form = EntryForm.newEntry(this.form.kind());
                    this.editingKey = Optional.empty();
                    setStatus("Entry removed from draft", StatusTone.WARNING);
                    rebuild();
                });
    }

    private void deleteEntry(CustomizationEntry entry) {
        boolean editing = this.editingKey.filter(entry.key()::equals).isPresent();
        Map<String, String> fieldText = editing ? Map.of() : snapshotFieldText();
        this.controller.remove(entry.key());
        if (editing) {
            this.form = EntryForm.newEntry(entry.kind());
            this.editingKey = Optional.empty();
            this.ingredientPage = 0;
        }
        setStatus("Entry removed from draft", StatusTone.WARNING);
        rebuild();
        if (!editing) {
            restoreFieldText(fieldText);
        }
    }

    private void filterEntries(String value) {
        this.entryQuery = value;
        this.entryPage = 0;
        requestNavigationRefresh();
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private void rebuildPreservingFieldText() {
        Map<String, String> values = snapshotFieldText();
        rebuild();
        restoreFieldText(values);
    }

    private Map<String, String> snapshotFieldText() {
        Map<String, String> values = new HashMap<>();
        this.fields.forEach((key, field) -> values.put(key, field.getValue()));
        return values;
    }

    private void restoreFieldText(Map<String, String> values) {
        values.forEach((key, value) -> Optional.ofNullable(this.fields.get(key))
                .ifPresent(field -> field.setValue(value)));
    }

    private void restoreFieldTextOnReopen() {
        if (this.fieldTextOnReopen.isEmpty()) {
            return;
        }
        Map<String, String> values = this.fieldTextOnReopen;
        this.fieldTextOnReopen = Map.of();
        restoreFieldText(values);
    }

    private void setStatus(String message, StatusTone tone) {
        this.status = Objects.requireNonNull(message, "message");
        this.statusTone = Objects.requireNonNull(tone, "tone");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || this.contextMenu != null) {
            this.tableSlotDrag.cancel();
        }
        if (this.contextMenu != null) {
            EditorContextMenu menu = this.contextMenu;
            if (menu.mouseClicked(mouseX, mouseY, button)) {
                if (!menu.isOpen() && this.contextMenu == menu) {
                    this.contextMenu = null;
                }
                return true;
            }
            menu.close();
            this.contextMenu = null;
            if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !this.tableBrush.active()) {
            int slot = tableSlotAt(mouseX, mouseY);
            boolean shapeless = this.form.kind() == EntryKind.SHAPELESS_TABLE;
            boolean sourcePresent = slot >= 0 && tableIngredientAt(slot, shapeless).isPresent();
            if (this.tableSlotDrag.begin(slot, sourcePresent, gestureMillis())) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.contextMenu != null) {
            EditorContextMenu menu = this.contextMenu;
            menu.keyPressed(keyCode);
            if (!menu.isOpen() && this.contextMenu == menu) {
                this.contextMenu = null;
            }
            return true;
        }
        if (this.minecraft != null && FocusedEditBoxKeyGuard.consume(
                this.getFocused(), this.minecraft.options.keyInventory,
                keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.contextMenu != null || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (this.contextMenu != null) {
            if (!this.contextMenu.mouseScrolled(mouseX, mouseY, delta)) {
                this.contextMenu.close();
                this.contextMenu = null;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (this.contextMenu != null) {
            this.tableSlotDrag.cancel();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.tableSlotDrag.armed()) {
            this.tableSlotDrag.updateTarget(tableSlotAt(mouseX, mouseY));
            return true;
        }
        if (button == 0 && this.tableBrush.active()) {
            int slot = tableSlotAt(mouseX, mouseY);
            if (slot >= 0) {
                if (slot != this.lastBrushedSlot) {
                    applyTableBrush(slot);
                }
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.lastBrushedSlot = -1;
        if (this.contextMenu != null) {
            this.tableSlotDrag.cancel();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.tableSlotDrag.armed()) {
            TableSlotDragGesture.Release release = this.tableSlotDrag.release(
                    tableSlotAt(mouseX, mouseY), gestureMillis());
            handleTableSlotRelease(release);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void handleTableSlotRelease(TableSlotDragGesture.Release release) {
        switch (release.action()) {
            case CLICK -> {
                if (this.form.kind().usesShapedGrid()) {
                    handleShapedSlot(release.sourceSlot());
                } else if (this.form.kind() == EntryKind.SHAPELESS_TABLE) {
                    handleShapelessSlot(release.sourceSlot());
                }
            }
            case SWAP -> applyTableSlotSwap(release.sourceSlot(), release.targetSlot());
            case NONE -> {
            }
        }
    }

    private void applyTableSlotSwap(int sourceSlot, int targetSlot) {
        TableGridOperations.SwapResult result = TableGridOperations.swap(
                this.form, sourceSlot, targetSlot);
        String key = switch (result) {
            case SWAPPED -> "gui.avaritia_tweak.drag_swap.swapped";
            case MOVED -> "gui.avaritia_tweak.drag_swap.moved";
            case UNCHANGED -> "gui.avaritia_tweak.drag_swap.unchanged";
            case INVALID -> "gui.avaritia_tweak.drag_swap.invalid";
        };
        StatusTone tone = result.changed() ? StatusTone.SUCCESS : StatusTone.MUTED;
        setStatus(Component.translatable(key, sourceSlot + 1, targetSlot + 1).getString(), tone);
    }

    private int tableSlotAt(double mouseX, double mouseY) {
        if (this.tableGridLayout == null || this.tableGridKind != this.form.kind()) {
            return -1;
        }
        return this.tableGridLayout.slotAt(mouseX, mouseY, this.form.tier().gridSize());
    }

    private static long gestureMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, EditorTheme.headerBackground());
        graphics.fill(0, HEADER_HEIGHT - 2, this.width, HEADER_HEIGHT, EditorTheme.AVARITIA_RED);
        if (this.wideLayout) {
            graphics.blit(TABLE_ICON, 9, 8, 0, 0, 16, 16, 16, 16);
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.editor_title"),
                    31, 8, EditorTheme.AVARITIA_GOLD, false);
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.editor_subtitle"),
                    31, 19, EditorTheme.TEXT_MUTED, false);
            String mode = Component.translatable("gui.avaritia_tweak.client_local").getString();
            int modeX = this.themeButtonX - 8 - this.font.width(mode);
            if (modeX > 180) {
                graphics.drawString(this.font, mode, modeX, 13, EditorTheme.AVARITIA_CYAN, false);
            }
        }
        if (this.wideLayout) {
            renderPanel(graphics, Panel.NAVIGATION, this.navigationX, this.navigationWidth);
            renderPanel(graphics, Panel.EDITOR, this.editorX, this.editorWidth);
            renderPanel(graphics, Panel.OUTPUT, this.outputX, this.outputWidth);
        } else {
            int x = switch (this.activePanel) {
                case NAVIGATION -> this.navigationX;
                case EDITOR -> this.editorX;
                case OUTPUT -> this.outputX;
            };
            int width = switch (this.activePanel) {
                case NAVIGATION -> this.navigationWidth;
                case EDITOR -> this.editorWidth;
                case OUTPUT -> this.outputWidth;
            };
            renderPanel(graphics, this.activePanel, x, width);
        }
        for (Label label : this.labels) {
            graphics.drawString(this.font, label.text, label.x, label.y, label.color, false);
        }
        for (Label label : this.navigationResultLabels) {
            graphics.drawString(this.font, label.text, label.x, label.y, label.color, false);
        }
        String leftStatus;
        if (this.tableSlotDrag.armed()) {
            String key = this.tableSlotDrag.active(gestureMillis())
                    ? "gui.avaritia_tweak.drag_swap.drag_hint"
                    : "gui.avaritia_tweak.drag_swap.hold_hint";
            leftStatus = Component.translatable(key).getString();
        } else {
            leftStatus = this.status.isEmpty()
                    ? Component.translatable("gui.avaritia_tweak.ready").getString()
                    : this.status;
        }
        String centerStatus = kindLabel(this.form.kind()).getString() + "  •  "
                + targetLabel(this.form.target()).getString();
        String rightStatus = Component.translatable("gui.avaritia_tweak.workspace_status",
                this.controller.baseVersion(), this.controller.draft().entries().size()).getString();
        EditorTheme.renderStatusBar(graphics, this.font, this.height - 18, this.width,
                leftStatus, centerStatus, rightStatus, this.statusTone.color());
    }

    private void renderPanel(GuiGraphics graphics, Panel panel, int x, int width) {
        int accent = switch (panel) {
            case NAVIGATION -> EditorTheme.AVARITIA_CYAN;
            case EDITOR -> EditorTheme.AVARITIA_RED;
            case OUTPUT -> EditorTheme.AVARITIA_GOLD;
        };
        EditorTheme.renderPanel(graphics, x, this.panelTop, width,
                this.panelBottom - this.panelTop, accent);
        if (panel == Panel.EDITOR) {
            EditorTheme.renderCanvasGrid(graphics, x + 4, this.panelTop + PANEL_HEADER_HEIGHT + 2,
                    width - 8, this.panelBottom - this.panelTop - PANEL_HEADER_HEIGHT - 6);
        }
        WorkspaceDiff workspaceDiff = this.differ.diff(this.controller.committed(), this.controller.draft());
        String meta = switch (panel) {
            case NAVIGATION -> Integer.toString(this.controller.draft().entries().size());
            case EDITOR -> targetLabel(this.form.target()).getString();
            case OUTPUT -> Integer.toString(workspaceDiff.entries().size());
        };
        EditorTheme.renderSectionHeader(graphics, this.font, x + 2, this.panelTop + 2,
                width - 4, panel.label(), meta, accent);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (ChangeIndicator indicator : this.changeIndicators) {
            graphics.drawString(this.font, EditorTheme.changeMark(indicator.type),
                    indicator.x, indicator.y, EditorTheme.changeColor(indicator.type), false);
        }
        for (ChangeIndicator indicator : this.navigationResultIndicators) {
            graphics.drawString(this.font, EditorTheme.changeMark(indicator.type),
                    indicator.x, indicator.y, EditorTheme.changeColor(indicator.type), false);
        }
        renderTableSlotDragOverlay(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.contextMenu != null) {
            this.contextMenu.render(graphics, mouseX, mouseY);
        }
    }

    private void renderTableSlotDragOverlay(GuiGraphics graphics) {
        if (!this.tableSlotDrag.armed() || this.tableGridLayout == null
                || this.tableGridKind != this.form.kind()) {
            return;
        }
        boolean active = this.tableSlotDrag.active(gestureMillis());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        renderTableSlotOutline(graphics, this.tableSlotDrag.sourceSlot(),
                active ? EditorTheme.AVARITIA_CYAN : EditorTheme.AVARITIA_GOLD);
        int targetSlot = this.tableSlotDrag.targetSlot();
        if (active && targetSlot >= 0 && targetSlot != this.tableSlotDrag.sourceSlot()) {
            TableGridOperations.SwapResult result = TableGridOperations.inspectSwap(
                    this.form, this.tableSlotDrag.sourceSlot(), targetSlot);
            int color = switch (result) {
                case SWAPPED, MOVED -> EditorTheme.SUCCESS;
                case UNCHANGED -> EditorTheme.WARNING;
                case INVALID -> EditorTheme.ERROR;
            };
            renderTableSlotOutline(graphics, targetSlot, color);
        }
        graphics.pose().popPose();
    }

    private void renderTableSlotOutline(GuiGraphics graphics, int slot, int color) {
        int size = this.form.tier().gridSize();
        if (slot < 0 || slot >= size * size) {
            return;
        }
        int cellSize = this.tableGridLayout.cellSize();
        int x = this.tableGridLayout.gridX() + slot % size * cellSize;
        int y = this.tableGridLayout.gridY() + slot / size * cellSize;
        graphics.fill(x, y, x + cellSize, y + cellSize, EditorTheme.withAlpha(color, 0x36));
        graphics.fill(x, y, x + cellSize, y + 1, color);
        graphics.fill(x, y + cellSize - 1, x + cellSize, y + cellSize, color);
        graphics.fill(x, y, x + 1, y + cellSize, color);
        graphics.fill(x + cellSize - 1, y, x + cellSize, y + cellSize, color);
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        Map<String, String> values = snapshotFieldText();
        captureFields();
        super.resize(minecraft, width, height);
        restoreFieldText(values);
    }

    @Override
    public void removed() {
        this.controller.flushDraft();
        super.removed();
    }

    void refreshFromController() {
        Optional<CustomizationEntry> selected = this.controller.selectedEntry()
                .map(this.controller.draft().entries()::get);
        if (selected.isPresent()) {
            this.form = EntryForm.from(selected.get());
            this.editingKey = Optional.of(selected.get().key());
        } else {
            this.form = EntryForm.newEntry(this.form.kind());
            this.editingKey = Optional.empty();
        }
        this.ingredientPage = 0;
    }

    private static Component kindLabel(EntryKind kind) {
        return Component.translatable("gui.avaritia_tweak.kind." + kind.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static Component targetLabel(OutputTarget target) {
        return Component.translatable("gui.avaritia_tweak.target." + target.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static Component toggleLabel(String field, boolean value) {
        return Component.literal(field + ": " + (value ? "ON" : "OFF"));
    }

    private static Component colorLabel(String field, int color) {
        return Component.translatable("gui.avaritia_tweak." + field)
                .append(Component.literal("  " + String.format(java.util.Locale.ROOT,
                        "#%06X", color & 0x00ffffff)));
    }

    private static List<OutputTarget> supportedTargets(EntryKind kind) {
        return OutputTarget.compatibleWith(kind);
    }

    private static <T> T cycle(T[] values, T current) {
        int index = java.util.Arrays.asList(values).indexOf(current);
        return values[(index + 1) % values.length];
    }

    private enum Panel {
        NAVIGATION("navigation"),
        EDITOR("editor"),
        OUTPUT("output");

        private final String id;

        Panel(String id) {
            this.id = id;
        }

        Component label() {
            return Component.translatable("gui.avaritia_tweak.panel." + this.id);
        }

        static Panel from(String value) {
            for (Panel panel : values()) {
                if (panel.id.equals(value)) {
                    return panel;
                }
            }
            return EDITOR;
        }
    }

    private enum StatusTone {
        MUTED,
        SUCCESS,
        WARNING,
        ERROR;

        int color() {
            return switch (this) {
                case MUTED -> EditorTheme.TEXT_MUTED;
                case SUCCESS -> EditorTheme.SUCCESS;
                case WARNING -> EditorTheme.WARNING;
                case ERROR -> EditorTheme.ERROR;
            };
        }
    }

    private record Label(int x, int y, Component text, int color) {
    }

    private record ChangeIndicator(int x, int y, ChangeType type) {
    }
}
