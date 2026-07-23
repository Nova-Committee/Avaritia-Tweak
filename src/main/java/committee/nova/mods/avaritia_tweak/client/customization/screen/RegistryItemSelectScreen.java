package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class RegistryItemSelectScreen extends Screen {
    private static final int ACTIVITY_RAIL_WIDTH = 58;
    private static final int CELL_SIZE = 22;

    private final Screen previous;
    private final Consumer<ResourceLocation> onSelected;
    private final List<ResourceLocation> allItems;
    private final List<ResourceLocation> filteredItems = new ArrayList<>();
    private final List<GhostItemStackButton> resultWidgets = new ArrayList<>();
    private final DeferredSearchRefresh searchRefresh = new DeferredSearchRefresh();
    private EditBox searchBox;
    private EditorButton previousPage;
    private EditorButton nextPage;
    private String query = "";
    private int page;

    RegistryItemSelectScreen(Screen previous, Consumer<ResourceLocation> onSelected) {
        super(Component.translatable("gui.avaritia_tweak.item_browser.title"));
        this.previous = previous;
        this.onSelected = onSelected;
        this.allItems = ForgeRegistries.ITEMS.getKeys().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        this.filteredItems.addAll(this.allItems);
    }

    @Override
    protected void init() {
        Layout layout = layout();
        this.resultWidgets.clear();
        this.searchBox = new EditBox(this.font, layout.gridX, layout.top + 38,
                layout.gridWidth, 20, Component.translatable("gui.avaritia_tweak.search_items"));
        this.searchBox.setValue(this.query);
        this.searchBox.setHint(Component.translatable("gui.avaritia_tweak.search_items"));
        this.searchBox.setResponder(this::filter);
        this.addRenderableWidget(this.searchBox);

        int actionY = layout.top + layout.height - 30;
        this.previousPage = this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            requestResultRefresh();
        }).bounds(layout.gridX, actionY, 34, 20).style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                        button -> onClose())
                .bounds(layout.left + layout.width - 90, actionY, 80, 20)
                .style(EditorButton.Style.QUIET).build());
        this.nextPage = this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
            this.page = Math.min(maxPage(pageSize(layout)), this.page + 1);
            requestResultRefresh();
        }).bounds(layout.left + layout.width - 130, actionY, 34, 20)
                .style(EditorButton.Style.QUIET).build());
        refreshResults();
        this.setInitialFocus(this.searchBox);
        this.searchBox.setCursorPosition(this.query.length());
    }

    private void select(ResourceLocation id) {
        this.onSelected.accept(id);
        Minecraft.getInstance().setScreen(this.previous);
    }

    private void filter(String value) {
        this.query = value;
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        this.filteredItems.clear();
        this.allItems.stream()
                .filter(id -> normalized.isEmpty()
                        || id.toString().toLowerCase(Locale.ROOT).contains(normalized))
                .forEach(this.filteredItems::add);
        this.page = 0;
        requestResultRefresh();
    }

    private void refreshResults() {
        this.resultWidgets.forEach(this::removeWidget);
        this.resultWidgets.clear();
        Layout layout = layout();
        int pageSize = pageSize(layout);
        int maxPage = maxPage(pageSize);
        this.page = Math.min(this.page, maxPage);
        int start = this.page * pageSize;
        int end = Math.min(this.filteredItems.size(), start + pageSize);
        for (int index = start; index < end; index++) {
            ResourceLocation id = this.filteredItems.get(index);
            int local = index - start;
            int x = layout.gridX + 2 + (local % layout.columns) * CELL_SIZE;
            int y = layout.gridTop + 2 + (local / layout.columns) * CELL_SIZE;
            GhostItemStackButton widget = new GhostItemStackButton(x, y,
                    () -> new ItemStackSpec(id, 1), button -> select(id));
            this.resultWidgets.add(this.addRenderableWidget(widget));
        }
        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page < maxPage;
    }

    private void requestResultRefresh() {
        this.searchRefresh.request(this, this::refreshResults);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();
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
                EditorTheme.AVARITIA_CYAN);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.AVARITIA_GOLD, false);
        String count = Component.translatable("gui.avaritia_tweak.item_browser.matches",
                this.filteredItems.size()).getString();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(count) - 22,
                layout.top + 9, count, EditorTheme.AVARITIA_CYAN);

        graphics.fill(layout.left + 2, layout.top + 34,
                layout.left + ACTIVITY_RAIL_WIDTH, layout.top + layout.height - 38,
                EditorTheme.PANEL_DARK);
        graphics.fill(layout.left + ACTIVITY_RAIL_WIDTH, layout.top + 34,
                layout.left + ACTIVITY_RAIL_WIDTH + 1, layout.top + layout.height - 38,
                EditorTheme.BORDER);
        renderActivityRail(graphics, layout);

        graphics.fill(layout.gridX - 3, layout.gridTop - 3,
                layout.gridX + layout.gridWidth + 3, layout.gridBottom + 3, EditorTheme.BORDER_DARK);
        graphics.fill(layout.gridX - 2, layout.gridTop - 2,
                layout.gridX + layout.gridWidth + 2, layout.gridBottom + 2, EditorTheme.PANEL);
        EditorTheme.renderCanvasGrid(graphics, layout.gridX, layout.gridTop,
                layout.gridWidth, layout.gridBottom - layout.gridTop);
        if (this.filteredItems.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.avaritia_tweak.item_browser.empty"),
                    layout.gridX + layout.gridWidth / 2, layout.gridTop + 28,
                    EditorTheme.TEXT_MUTED);
        }

        int pageSize = pageSize(layout);
        graphics.drawCenteredString(this.font,
                (this.page + 1) + "/" + (maxPage(pageSize) + 1),
                layout.gridX + layout.gridWidth / 2,
                layout.top + layout.height - 24, EditorTheme.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderActivityRail(GuiGraphics graphics, Layout layout) {
        int center = layout.left + ACTIVITY_RAIL_WIDTH / 2;
        graphics.fill(layout.left + 10, layout.top + 46,
                layout.left + ACTIVITY_RAIL_WIDTH - 9, layout.top + 82, EditorTheme.BORDER_DARK);
        graphics.fill(layout.left + 11, layout.top + 47,
                layout.left + ACTIVITY_RAIL_WIDTH - 10, layout.top + 81, 0xff31474b);
        graphics.fill(layout.left + 11, layout.top + 47,
                layout.left + 14, layout.top + 81, EditorTheme.AVARITIA_CYAN);
        graphics.drawCenteredString(this.font, "ITEM", center, layout.top + 58, EditorTheme.TEXT);
        graphics.drawCenteredString(this.font, "ID", center, layout.top + 69, EditorTheme.AVARITIA_CYAN);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.avaritia_tweak.item_browser.registry"),
                center, layout.top + 94, EditorTheme.TEXT_FAINT);
        graphics.drawCenteredString(this.font, Integer.toString(this.allItems.size()),
                center, layout.top + 107, EditorTheme.TEXT_MUTED);
    }

    private int pageSize(Layout layout) {
        return Math.max(1, layout.columns * layout.rows);
    }

    private int maxPage(int pageSize) {
        return Math.max(0, (this.filteredItems.size() - 1) / pageSize);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 620, 360);
        int panelWidth = frame.width();
        int panelHeight = frame.height();
        int left = frame.left();
        int top = frame.top();
        int gridX = left + ACTIVITY_RAIL_WIDTH + 12;
        int gridWidth = panelWidth - ACTIVITY_RAIL_WIDTH - 22;
        int gridTop = top + 66;
        int gridBottom = top + panelHeight - 42;
        int columns = Math.max(1, (gridWidth - 4) / CELL_SIZE);
        int rows = Math.max(1, (gridBottom - gridTop - 4) / CELL_SIZE);
        return new Layout(left, top, panelWidth, panelHeight, gridX, gridWidth,
                gridTop, gridBottom, columns, rows);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private record Layout(int left, int top, int width, int height,
                          int gridX, int gridWidth, int gridTop, int gridBottom,
                          int columns, int rows) {
    }
}
