package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Selects an effective Avaritia singularity or a definition staged in the local workspace. */
final class SingularitySelectScreen extends Screen {
    private final Screen previous;
    private final Consumer<ResourceLocation> onSelected;
    private final List<Choice> allChoices;
    private final List<Choice> filteredChoices = new ArrayList<>();
    private final List<EditorButton> resultWidgets = new ArrayList<>();
    private final List<VisibleChoice> visibleChoices = new ArrayList<>();
    private final DeferredSearchRefresh searchRefresh = new DeferredSearchRefresh();
    private EditBox searchBox;
    private EditorButton previousPage;
    private EditorButton nextPage;
    private String query = "";
    private int page;

    SingularitySelectScreen(Screen previous, WorkspaceSnapshot draft,
                            Consumer<ResourceLocation> onSelected) {
        super(Component.translatable("gui.avaritia_tweak.singularity_browser.title"));
        this.previous = previous;
        this.onSelected = onSelected;
        Map<ResourceLocation, Choice> effective = new TreeMap<>();
        SingularityReloadListener.INSTANCE.getAllSingularities().forEach((id, singularity) ->
                effective.put(id, fromRuntime(singularity)));
        this.allChoices = collectChoices(effective, draft);
        this.filteredChoices.addAll(this.allChoices);
    }

    @Override
    protected void init() {
        Layout layout = layout();
        this.resultWidgets.clear();
        this.visibleChoices.clear();
        this.searchBox = new EditBox(this.font, layout.left + 8, layout.top + 38,
                layout.width - 16, 20,
                Component.translatable("gui.avaritia_tweak.singularity_browser.search"));
        this.searchBox.setMaxLength(256);
        this.searchBox.setValue(this.query);
        this.searchBox.setHint(Component.translatable("gui.avaritia_tweak.singularity_browser.search"));
        this.searchBox.setResponder(this::filter);
        this.addRenderableWidget(this.searchBox);

        int actionY = layout.top + layout.height - 30;
        this.previousPage = this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            requestResultRefresh();
        }).bounds(layout.left + 8, actionY, 34, 20).style(EditorButton.Style.QUIET).build());
        this.nextPage = this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
            this.page = Math.min(maxPage(pageSize(layout)), this.page + 1);
            requestResultRefresh();
        }).bounds(layout.left + 48, actionY, 34, 20).style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                        button -> onClose())
                .bounds(layout.left + layout.width - 90, actionY, 80, 20)
                .style(EditorButton.Style.QUIET).build());

        refreshResults();
        this.setInitialFocus(this.searchBox);
        this.searchBox.setCursorPosition(this.query.length());
    }

    private void filter(String value) {
        this.query = value;
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        this.filteredChoices.clear();
        this.allChoices.stream()
                .filter(choice -> normalized.isEmpty()
                        || choice.id.toString().toLowerCase(Locale.ROOT).contains(normalized)
                        || choice.displayName.toLowerCase(Locale.ROOT).contains(normalized)
                        || localizedName(choice).toLowerCase(Locale.ROOT).contains(normalized)
                        || sourceName(choice).toLowerCase(Locale.ROOT).contains(normalized))
                .forEach(this.filteredChoices::add);
        this.page = 0;
        requestResultRefresh();
    }

    private void refreshResults() {
        this.resultWidgets.forEach(this::removeWidget);
        this.resultWidgets.clear();
        this.visibleChoices.clear();
        Layout layout = layout();
        int pageSize = pageSize(layout);
        int maxPage = maxPage(pageSize);
        this.page = Math.min(this.page, maxPage);
        int start = this.page * pageSize;
        int end = Math.min(this.filteredChoices.size(), start + pageSize);
        for (int index = start; index < end; index++) {
            Choice choice = this.filteredChoices.get(index);
            int y = layout.listTop + (index - start) * 22;
            String label = "[" + sourceName(choice) + "]  "
                    + localizedName(choice) + "  ·  " + choice.id;
            EditorButton row = EditorButton.builder(
                            Component.literal(ScreenText.fit(this.font, label, layout.width - 66)),
                            button -> select(choice.id))
                    .bounds(layout.left + 34, y, layout.width - 42, 20)
                    .style(EditorButton.Style.LIST).build();
            this.resultWidgets.add(this.addRenderableWidget(row));
            this.visibleChoices.add(new VisibleChoice(choice, layout.left + 10, y + 3));
        }
        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page < maxPage;
    }

    private void requestResultRefresh() {
        this.searchRefresh.request(this, this::refreshResults);
    }

    private void select(ResourceLocation id) {
        this.onSelected.accept(id);
        Minecraft.getInstance().setScreen(this.previous);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();
        int next = Math.max(0, Math.min(maxPage(pageSize(layout)),
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
        String count = Component.translatable("gui.avaritia_tweak.singularity_browser.matches",
                this.filteredChoices.size()).getString();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(count) - 22,
                layout.top + 9, count, EditorTheme.AVARITIA_CYAN);
        graphics.fill(layout.left + 6, layout.listTop - 4,
                layout.left + layout.width - 6, layout.listBottom + 2, EditorTheme.BORDER_DARK);
        graphics.fill(layout.left + 7, layout.listTop - 3,
                layout.left + layout.width - 7, layout.listBottom + 1, EditorTheme.PANEL);
        EditorTheme.renderCanvasGrid(graphics, layout.left + 8, layout.listTop - 2,
                layout.width - 16, layout.listBottom - layout.listTop + 2);
        for (VisibleChoice visible : this.visibleChoices) {
            graphics.fill(visible.x, visible.y, visible.x + 8, visible.y + 14,
                    0xff000000 | visible.choice.underlayColor);
            graphics.fill(visible.x + 9, visible.y, visible.x + 17, visible.y + 14,
                    0xff000000 | visible.choice.overlayColor);
            int sourceColor = visible.choice.source == Source.DRAFT
                    ? EditorTheme.AVARITIA_GOLD : EditorTheme.AVARITIA_CYAN;
            graphics.fill(visible.x + 19, visible.y, visible.x + 21, visible.y + 14, sourceColor);
        }
        if (this.filteredChoices.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.avaritia_tweak.singularity_browser.empty"),
                    layout.left + layout.width / 2, layout.listTop + 28, EditorTheme.TEXT_MUTED);
        }
        graphics.drawCenteredString(this.font,
                (this.page + 1) + "/" + (maxPage(pageSize(layout)) + 1),
                layout.left + layout.width / 2, layout.top + layout.height - 24,
                EditorTheme.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private int pageSize(Layout layout) {
        return Math.max(1, (layout.listBottom - layout.listTop) / 22);
    }

    private int maxPage(int pageSize) {
        return Math.max(0, (this.filteredChoices.size() - 1) / pageSize);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 620, 360);
        int listTop = frame.top() + 66;
        int listBottom = frame.top() + frame.height() - 42;
        return new Layout(frame.left(), frame.top(), frame.width(), frame.height(), listTop, listBottom);
    }

    static List<Choice> collectChoices(Map<ResourceLocation, Choice> effective,
                                       WorkspaceSnapshot draft) {
        Map<ResourceLocation, Choice> indexed = new TreeMap<>(effective);
        draft.entries().values().forEach(entry -> {
            if (entry instanceof CustomizationEntry.SingularityDefinition definition) {
                indexed.put(definition.id(), new Choice(definition.id(), definition.displayName(),
                        definition.overlayColor(), definition.underlayColor(), Source.DRAFT));
            }
        });
        return List.copyOf(indexed.values());
    }

    private static Choice fromRuntime(Singularity singularity) {
        return new Choice(singularity.getRegistryName(), singularity.getDisplayName(),
                singularity.getOverlayColor(), singularity.getUnderlayColor(), Source.EFFECTIVE);
    }

    private static String localizedName(Choice choice) {
        return Component.translatable(choice.displayName).getString();
    }

    private static String sourceName(Choice choice) {
        return Component.translatable("gui.avaritia_tweak.singularity_browser.source."
                + choice.source.name().toLowerCase(Locale.ROOT)).getString();
    }

    enum Source {
        EFFECTIVE,
        DRAFT
    }

    record Choice(ResourceLocation id, String displayName, int overlayColor,
                  int underlayColor, Source source) {
    }

    private record VisibleChoice(Choice choice, int x, int y) {
    }

    private record Layout(int left, int top, int width, int height, int listTop, int listBottom) {
    }
}
