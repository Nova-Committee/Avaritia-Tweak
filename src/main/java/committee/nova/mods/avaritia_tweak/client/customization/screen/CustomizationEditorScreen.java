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
    private static final int ENTRY_PAGE_SIZE = 8;
    private static final int INGREDIENT_PAGE_SIZE = 18;
    private final EditorController controller;
    private final WorkspaceDiffer differ = new WorkspaceDiffer();
    private final Map<String, EditBox> fields = new HashMap<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<ChangeIndicator> changeIndicators = new ArrayList<>();
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
    private int gridPage;
    private String status = "";
    private int statusColor = 0xffaeb6c6;
    private String entryQuery = "";
    private boolean navigationRebuildQueued;
    private boolean focusEntrySearch;

    public CustomizationEditorScreen(RecipeGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        ClientCustomizationServices services = ClientCustomizationServices.get();
        this.controller = services.controller();
        services.startupFailure().ifPresent(failure -> {
            this.status = CommitResultMessages.describe(failure);
            this.statusColor = 0xffff6b6b;
        });
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
        this.wideLayout = this.width >= 680 && this.height >= 300;
        this.panelTop = HEADER_HEIGHT + 2;
        this.panelBottom = this.height - FOOTER_HEIGHT;
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
    }

    private void addPanelTabs() {
        int width = Math.max(70, (this.width - 20) / 3);
        for (int index = 0; index < Panel.values().length; index++) {
            Panel panel = Panel.values()[index];
            this.addRenderableWidget(EditorButton.builder(panel.label(), button -> {
                captureFields();
                this.activePanel = panel;
                this.controller.activePanel(panel.id);
                rebuild();
            }).bounds(8 + index * width, 7, width - 2, 20)
                    .style(EditorButton.Style.TAB).selected(panel == this.activePanel).build());
        }
    }

    private void addNavigationPanel() {
        int x = this.navigationX + 8;
        int y = this.panelTop + PANEL_HEADER_HEIGHT + 8;
        int width = this.navigationWidth - 16;
        this.addRenderableWidget(EditorButton.builder(kindLabel(this.form.kind()), button -> {
            EntryKind next = cycle(EntryKind.values(), this.form.kind());
            this.form = EntryForm.newEntry(next);
            this.editingKey = Optional.empty();
            this.ingredientPage = 0;
            this.gridPage = 0;
            rebuild();
        }).bounds(x, y, width, 20).style(EditorButton.Style.DEFAULT).build());
        y += 24;
        int gap = 3;
        int actionWidth = (width - gap * 2) / 3;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.new_entry"), button -> {
            this.form = EntryForm.newEntry(this.form.kind());
            this.editingKey = Optional.empty();
            this.status = "New local form";
            rebuild();
        }).bounds(x, y, actionWidth, 20).style(EditorButton.Style.PRIMARY).build());
        EditorButton duplicate = this.addRenderableWidget(EditorButton.builder(
                Component.translatable("gui.avaritia_tweak.duplicate_entry"), button -> duplicateSelected())
                .bounds(x + actionWidth + gap, y, actionWidth, 20).style(EditorButton.Style.QUIET).build());
        duplicate.active = this.editingKey
                .map(this.controller.draft().entries()::containsKey)
                .orElse(false);
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.import_recipe"),
                button -> openRecipeImporter()).bounds(x + (actionWidth + gap) * 2, y,
                width - (actionWidth + gap) * 2, 20).style(EditorButton.Style.QUIET).build());
        y += 24;

        EditBox searchBox = new EditBox(this.font, x, y, width, 18,
                Component.translatable("gui.avaritia_tweak.search_entries"));
        searchBox.setMaxLength(256);
        searchBox.setValue(this.entryQuery);
        searchBox.setHint(Component.translatable("gui.avaritia_tweak.search_entries"));
        searchBox.setResponder(this::filterEntries);
        this.addRenderableWidget(searchBox);
        if (this.focusEntrySearch) {
            this.focusEntrySearch = false;
            this.setInitialFocus(searchBox);
            searchBox.setCursorPosition(this.entryQuery.length());
        }
        y += 24;

        List<CustomizationEntry> entries = EntryNavigation.search(
                this.controller.draft().entries().values(), this.entryQuery);
        int availableRows = Math.max(1, (this.panelBottom - y - 26) / 20);
        int pageSize = Math.min(ENTRY_PAGE_SIZE, availableRows);
        int maxPage = Math.max(0, (entries.size() - 1) / pageSize);
        this.entryPage = Math.min(this.entryPage, maxPage);
        int start = this.entryPage * pageSize;
        WorkspaceDiff workspaceDiff = this.differ.diff(this.controller.committed(), this.controller.draft());
        for (int index = start; index < Math.min(entries.size(), start + pageSize); index++) {
            CustomizationEntry entry = entries.get(index);
            String label = ScreenText.ellipsize(entry.id().toString(), 24);
            boolean selected = this.editingKey.filter(entry.key()::equals).isPresent();
            int rowY = y;
            this.addRenderableWidget(EditorButton.builder(Component.literal(label), button -> selectEntry(entry))
                    .bounds(x, rowY, width, 18).style(EditorButton.Style.LIST).selected(selected).build());
            workspaceDiff.entries().stream().filter(change -> change.key().equals(entry.key())).findFirst()
                    .ifPresent(change -> this.changeIndicators.add(
                            new ChangeIndicator(x + width - 11, rowY + 5, change.type())));
            y += 20;
        }
        if (entries.isEmpty()) {
            this.labels.add(new Label(x, y + 4, Component.translatable("gui.avaritia_tweak.no_entries"),
                    0xff7f899b));
            y += 24;
        }
        if (maxPage > 0) {
            int pageY = this.panelBottom - 22;
            this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
                this.entryPage = Math.max(0, this.entryPage - 1);
                rebuild();
            }).bounds(x, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
            this.labels.add(new Label(x + 38, pageY + 5,
                    Component.literal((this.entryPage + 1) + "/" + (maxPage + 1)), 0xffaeb6c6));
            this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
                this.entryPage = Math.min(maxPage, this.entryPage + 1);
                rebuild();
            }).bounds(x + width - 30, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
        }
    }

    private void addEditorPanel() {
        int x = this.editorX + 10;
        int y = this.panelTop + PANEL_HEADER_HEIGHT + 8;
        int innerWidth = this.editorWidth - 20;
        addField("id", Component.translatable("gui.avaritia_tweak.entry_id"),
                this.form.idText(), x, y, Math.min(260, innerWidth));
        y += 30;
        int buttonWidth = Math.min(150, innerWidth / 2);
        this.addRenderableWidget(EditorButton.builder(targetLabel(this.form.target()), button -> {
            captureFields();
            List<OutputTarget> targets = supportedTargets(this.form.kind());
            int current = targets.indexOf(this.form.target());
            this.form.target(targets.get((current + 1) % targets.size()));
            rebuild();
        }).bounds(x, y, buttonWidth, 20).style(EditorButton.Style.QUIET).build());
        this.labels.add(new Label(x + buttonWidth + 8, y + 6, kindLabel(this.form.kind()), 0xffd9b565));
        y += 28;

        switch (this.form.kind()) {
            case SHAPED_TABLE -> addShapedEditor(x, y, innerWidth);
            case SHAPELESS_TABLE -> addShapelessEditor(x, y, innerWidth);
            case COMPRESSOR -> addCompressorEditor(x, y, innerWidth);
            case EXTREME_SMITHING -> addSmithingEditor(x, y, innerWidth);
            case INFINITY_CATALYST -> addCatalystEditor(x, y, innerWidth);
            case ETERNAL_SINGULARITY -> addEternalEditor(x, y, innerWidth);
            case SINGULARITY_DEFINITION -> addSingularityEditor(x, y, innerWidth);
            case SINGULARITY_OPERATION -> addOperationEditor(x, y, innerWidth);
        }

        int actionsY = this.panelBottom - 28;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.stage"), button -> {
            if (stageForm()) {
                this.status = "Staged " + this.form.idText();
                this.statusColor = 0xff78d6a3;
                if (!this.wideLayout) {
                    this.activePanel = Panel.OUTPUT;
                    this.controller.activePanel(this.activePanel.id);
                }
                rebuild();
            }
        }).bounds(x, actionsY, 92, 20).style(EditorButton.Style.PRIMARY).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.delete"), button -> {
            this.editingKey.ifPresent(this.controller::remove);
            this.form = EntryForm.newEntry(this.form.kind());
            this.editingKey = Optional.empty();
            this.status = "Entry removed from draft";
            rebuild();
        }).bounds(x + 98, actionsY, 82, 20).style(EditorButton.Style.DANGER).build());
    }

    private void addShapedEditor(int x, int y, int width) {
        addTierButton(x, y);
        this.labels.add(new Label(x + 104, y + 6,
                Component.literal(this.form.tier().gridSize() + "×" + this.form.tier().gridSize()),
                0xffaeb6c6));
        y += 26;
        int size = this.form.tier().gridSize();
        int rowsPerPage = Math.min(5, size);
        int maxPage = Math.max(0, (size - 1) / rowsPerPage);
        this.gridPage = Math.min(this.gridPage, maxPage);
        int firstRow = this.gridPage * rowsPerPage;
        int lastRow = Math.min(size, firstRow + rowsPerPage);
        for (int row = firstRow; row < lastRow; row++) {
            for (int column = 0; column < size; column++) {
                int slot = row * size + column;
                this.addRenderableWidget(new GhostIngredientButton(x + column * 18,
                        y + (row - firstRow) * 18,
                        () -> Optional.ofNullable(this.form.grid().get(slot)),
                        button -> openIngredient(Optional.ofNullable(this.form.grid().get(slot)),
                                value -> this.form.gridIngredient(slot, value))));
            }
        }
        int resultX = x + Math.min(width - 24, size * 18 + 18);
        this.labels.add(new Label(resultX, y - 10, Component.translatable("gui.avaritia_tweak.result"),
                0xffaeb6c6));
        this.addRenderableWidget(new GhostItemStackButton(resultX, y,
                this.form::result, button -> openResult()));
        if (maxPage > 0) {
            int pageY = y + rowsPerPage * 18 + 3;
            addGridPagination(x, pageY, maxPage);
        }
    }

    private void addShapelessEditor(int x, int y, int width) {
        addTierButton(x, y);
        this.addRenderableWidget(new GhostItemStackButton(x + 112, y,
                this.form::result, button -> openResult()));
        this.labels.add(new Label(x + 138, y + 6, Component.translatable("gui.avaritia_tweak.result"),
                0xffaeb6c6));
        addIngredientList(x, y + 30, width, this.form.tier().capacity());
    }

    private void addCompressorEditor(int x, int y, int width) {
        addIngredientSlot(x, y, Component.translatable("gui.avaritia_tweak.input"),
                this.form.ingredient(), this.form::ingredient);
        this.addRenderableWidget(new GhostItemStackButton(x + 128, y,
                this.form::result, button -> openResult()));
        this.labels.add(new Label(x + 152, y + 6, Component.translatable("gui.avaritia_tweak.result"),
                0xffaeb6c6));
        addField("inputCount", Component.translatable("gui.avaritia_tweak.input_count"),
                Integer.toString(this.form.inputCount()), x, y + 36, 120);
        addField("timeCost", Component.translatable("gui.avaritia_tweak.time_cost"),
                Integer.toString(this.form.timeCost()), x + 138, y + 36, 120);
    }

    private void addSmithingEditor(int x, int y, int width) {
        int spacing = Math.max(48, (width - 18) / 4);
        addCompactIngredientSlot(x, y, Component.translatable("gui.avaritia_tweak.template"),
                this.form.template(), this.form::template);
        addCompactIngredientSlot(x + spacing, y, Component.translatable("gui.avaritia_tweak.base"),
                this.form.base(), this.form::base);
        addCompactIngredientSlot(x + spacing * 2, y,
                Component.translatable("gui.avaritia_tweak.addition"),
                this.form.addition(), this.form::addition);
        this.labels.add(new Label(x + spacing * 3, y,
                Component.translatable("gui.avaritia_tweak.result"), 0xffaeb6c6));
        this.addRenderableWidget(new GhostItemStackButton(x + spacing * 3, y + 11,
                this.form::result, button -> openResult()));
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
        addField("overlayColor", Component.translatable("gui.avaritia_tweak.overlay_color"),
                String.format("%06x", this.form.overlayColor()), x, y + 32, 112);
        addField("underlayColor", Component.translatable("gui.avaritia_tweak.underlay_color"),
                String.format("%06x", this.form.underlayColor()), x + 132, y + 32, 112);
        int numberWidth = Math.min(90, Math.max(60, (width - 54) / 2));
        addField("count", Component.translatable("gui.avaritia_tweak.count"),
                Integer.toString(this.form.count()), x, y + 64, numberWidth);
        addField("timeCost", Component.translatable("gui.avaritia_tweak.time_cost"),
                Integer.toString(this.form.timeCost()), x + numberWidth + 12, y + 64, numberWidth);
        int ingredientX = x + width - 18;
        this.labels.add(new Label(Math.max(x, ingredientX - 40), y + 64,
                Component.translatable("gui.avaritia_tweak.input"), 0xff8d96a8));
        this.addRenderableWidget(new GhostIngredientButton(ingredientX, y + 74,
                () -> Optional.of(this.form.ingredient()),
                button -> openIngredient(Optional.of(this.form.ingredient()),
                        value -> value.ifPresent(this.form::ingredient))));
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
            addField("singularityId", Component.translatable("gui.avaritia_tweak.singularity_id"),
                    this.form.singularityIdText(), x, y + 34, Math.min(260, width));
        } else {
            this.labels.add(new Label(x, y + 36,
                    Component.translatable("gui.avaritia_tweak.global_operation"), 0xfff0c66a));
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
                this.status = preview.validation().errors().isEmpty()
                        ? "Preview unavailable"
                        : preview.validation().errors().get(0).fieldPath() + ": "
                        + preview.validation().errors().get(0).messageKey();
                this.statusColor = 0xffff6b6b;
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
                    this.status = "Draft reset to committed version";
                    rebuild();
                }).bounds(x, discardY, width, 20).style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.clear_workspace"),
                button -> {
                    this.controller.clearDraft();
                    this.form = EntryForm.newEntry(this.form.kind());
                    this.editingKey = Optional.empty();
                    this.status = "All entries removed from draft";
                    rebuild();
                }).bounds(x, discardY + 24, width, 20).style(EditorButton.Style.DANGER).build());

        int warningY = Math.min(discardY - 14, y + 14);
        this.controller.draftWarning().ifPresent(warning -> this.labels.add(new Label(x,
                warningY, Component.literal(ScreenText.ellipsize(warning, 34)),
                EditorTheme.WARNING)));
    }

    private void addTierButton(int x, int y) {
        this.addRenderableWidget(EditorButton.builder(Component.literal("Tier " + this.form.tier().value()), button -> {
            captureFields();
            this.form.tier(cycle(CraftingTier.values(), this.form.tier()));
            this.gridPage = 0;
            rebuild();
        }).bounds(x, y, 96, 20).style(EditorButton.Style.QUIET).build());
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
                    })));
        }
        if (values.size() < maximum && limit - start < INGREDIENT_PAGE_SIZE) {
            int local = limit - start;
            this.addRenderableWidget(new GhostIngredientButton(x + (local % 9) * 22,
                    y + (local / 9) * 24, Optional::empty,
                    button -> openIngredient(Optional.empty(), value -> value.ifPresent(this.form::addIngredient))));
        }
        this.labels.add(new Label(x, y + 53,
                Component.literal(values.size() + "/" + maximum + " ingredients"), 0xff8d96a8));
        if (maxPage > 0 || values.size() >= INGREDIENT_PAGE_SIZE) {
            int pageY = y + 66;
            this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
                this.ingredientPage = Math.max(0, this.ingredientPage - 1);
                rebuild();
            }).bounds(x, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
            this.labels.add(new Label(x + 38, pageY + 5,
                    Component.literal((this.ingredientPage + 1) + "/" + (maxPage + 1)), 0xffaeb6c6));
            this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
                this.ingredientPage = Math.min(maxPage, this.ingredientPage + 1);
                rebuild();
            }).bounds(x + 90, pageY, 30, 18).style(EditorButton.Style.QUIET).build());
        }
    }

    private void addGridPagination(int x, int y, int maxPage) {
        this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
            this.gridPage = Math.max(0, this.gridPage - 1);
            rebuild();
        }).bounds(x, y, 30, 18).style(EditorButton.Style.QUIET).build());
        this.labels.add(new Label(x + 38, y + 5,
                Component.literal((this.gridPage + 1) + "/" + (maxPage + 1)), 0xffaeb6c6));
        this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
            this.gridPage = Math.min(maxPage, this.gridPage + 1);
            rebuild();
        }).bounds(x + 90, y, 30, 18).style(EditorButton.Style.QUIET).build());
    }

    private void addIngredientSlot(int x, int y, Component label, IngredientSpec current,
                                   Consumer<IngredientSpec> setter) {
        this.addRenderableWidget(new GhostIngredientButton(x, y, () -> Optional.of(current),
                button -> openIngredient(Optional.of(current),
                        value -> value.ifPresent(setter))));
        this.labels.add(new Label(x + 24, y + 6, label, 0xffaeb6c6));
    }

    private void addCompactIngredientSlot(int x, int y, Component label, IngredientSpec current,
                                          Consumer<IngredientSpec> setter) {
        this.labels.add(new Label(x, y, label, 0xffaeb6c6));
        this.addRenderableWidget(new GhostIngredientButton(x, y + 11, () -> Optional.of(current),
                button -> openIngredient(Optional.of(current), value -> value.ifPresent(setter))));
    }

    private void addField(String key, Component label, String value, int x, int y, int width) {
        this.labels.add(new Label(x, y, label, 0xff8d96a8));
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
            if (this.fields.containsKey("overlayColor")) {
                this.form.overlayColor(parseColor("overlayColor"));
            }
            if (this.fields.containsKey("underlayColor")) {
                this.form.underlayColor(parseColor("underlayColor"));
            }
            if (this.fields.containsKey("singularityId")) {
                this.form.singularityIdText(this.fields.get("singularityId").getValue().strip());
            }
            return true;
        } catch (NumberFormatException exception) {
            this.status = "Invalid number: " + exception.getMessage();
            this.statusColor = 0xffff6b6b;
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
            this.status = exception.fieldPath() + ": " + exception.getMessage();
            this.statusColor = 0xffff6b6b;
            return false;
        }
    }

    private int parseInt(String key) {
        return Integer.parseInt(this.fields.get(key).getValue().strip());
    }

    private int parseColor(String key) {
        String value = this.fields.get(key).getValue().strip().toLowerCase(java.util.Locale.ROOT);
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x")) {
            value = value.substring(2);
        }
        return Integer.parseInt(value, 16);
    }

    private void openIngredient(Optional<IngredientSpec> initial,
                                Consumer<Optional<IngredientSpec>> setter) {
        captureFields();
        Minecraft.getInstance().setScreen(new IngredientEditorScreen(this, initial, value -> {
            setter.accept(value);
            this.status = "Ghost ingredient updated locally";
        }));
    }

    private void openResult() {
        captureFields();
        Minecraft.getInstance().setScreen(new ItemStackEditorScreen(this, this.form.result(), result -> {
            this.form.result(result);
            this.status = "Ghost result updated locally";
        }));
    }

    private void openRecipeImporter() {
        captureFields();
        Minecraft.getInstance().setScreen(new RecipeSelectScreen(this, this.form.target(), result -> {
            if (result instanceof RecipeImportResult.Success success) {
                this.form = EntryForm.from(success.entry());
                this.editingKey = Optional.of(success.entry().key());
                this.status = "Recipe imported into local draft form";
                this.statusColor = 0xff78d6a3;
            } else {
                RecipeImportResult.Failure failure = (RecipeImportResult.Failure) result;
                this.status = failure.fieldPath() + ": " + failure.message();
                this.statusColor = 0xffff6b6b;
            }
        }));
    }

    private void selectEntry(CustomizationEntry entry) {
        captureFields();
        this.form = EntryForm.from(entry);
        this.editingKey = Optional.of(entry.key());
        this.controller.select(this.editingKey);
        this.ingredientPage = 0;
        this.gridPage = 0;
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
        this.form = EntryNavigation.duplicateForEditing(source.orElseThrow(),
                this.controller.draft().entries().values());
        this.editingKey = Optional.empty();
        this.ingredientPage = 0;
        this.gridPage = 0;
        this.status = Component.translatable("gui.avaritia_tweak.duplicate_ready", this.form.idText()).getString();
        this.statusColor = 0xff78d6a3;
        if (!this.wideLayout) {
            this.activePanel = Panel.EDITOR;
            this.controller.activePanel(this.activePanel.id);
        }
        rebuild();
    }

    private void filterEntries(String value) {
        this.entryQuery = value;
        this.entryPage = 0;
        if (!captureFields() || this.navigationRebuildQueued) {
            return;
        }
        this.navigationRebuildQueued = true;
        this.focusEntrySearch = true;
        Minecraft.getInstance().execute(() -> {
            this.navigationRebuildQueued = false;
            if (Minecraft.getInstance().screen == this) {
                rebuild();
            }
        });
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, 0xff1b1d23);
        graphics.fill(0, HEADER_HEIGHT - 2, this.width, HEADER_HEIGHT, EditorTheme.AVARITIA_RED);
        if (this.wideLayout) {
            graphics.blit(TABLE_ICON, 9, 8, 0, 0, 16, 16, 16, 16);
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.editor_title"),
                    31, 8, EditorTheme.AVARITIA_GOLD, false);
            graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.editor_subtitle"),
                    31, 19, EditorTheme.TEXT_MUTED, false);
            String mode = Component.translatable("gui.avaritia_tweak.client_local").getString();
            graphics.drawString(this.font, mode, this.width - 10 - this.font.width(mode), 13,
                    EditorTheme.AVARITIA_CYAN, false);
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
        String leftStatus = this.status.isEmpty()
                ? Component.translatable("gui.avaritia_tweak.ready").getString()
                : this.status;
        String centerStatus = kindLabel(this.form.kind()).getString() + "  •  "
                + targetLabel(this.form.target()).getString();
        String rightStatus = Component.translatable("gui.avaritia_tweak.workspace_status",
                this.controller.baseVersion(), this.controller.draft().entries().size()).getString();
        EditorTheme.renderStatusBar(graphics, this.font, this.height - 18, this.width,
                leftStatus, centerStatus, rightStatus, this.statusColor);
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
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        captureFields();
        super.resize(minecraft, width, height);
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
        this.gridPage = 0;
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

    private record Label(int x, int y, Component text, int color) {
    }

    private record ChangeIndicator(int x, int y, ChangeType type) {
    }
}
