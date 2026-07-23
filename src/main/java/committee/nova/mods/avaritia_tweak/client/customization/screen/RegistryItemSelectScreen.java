package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
    private static final int PAGE_SIZE = 8;
    private final Screen previous;
    private final Consumer<ResourceLocation> onSelected;
    private final List<ResourceLocation> allItems;
    private final List<ResourceLocation> filteredItems = new ArrayList<>();
    private EditBox searchBox;
    private String query = "";
    private int page;
    private boolean rebuildQueued;

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
        int panelWidth = Math.min(300, this.width - 12);
        int panelHeight = Math.min(230, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        this.searchBox = new EditBox(this.font, left + 8, top + 26,
                panelWidth - 16, 20, Component.translatable("gui.avaritia_tweak.search_items"));
        this.searchBox.setValue(this.query);
        this.searchBox.setResponder(this::filter);
        this.addRenderableWidget(this.searchBox);
        int start = this.page * PAGE_SIZE;
        int rowWidth = panelWidth - 16;
        for (int index = start; index < Math.min(this.filteredItems.size(), start + PAGE_SIZE); index++) {
            ResourceLocation id = this.filteredItems.get(index);
            this.addRenderableWidget(Button.builder(Component.literal(id.toString()), button -> {
                this.onSelected.accept(id);
                Minecraft.getInstance().setScreen(this.previous);
            }).bounds(left + 8, top + 50 + (index - start) * 18, rowWidth, 17).build());
        }
        int bottom = top + panelHeight - 22;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            rebuild();
        }).bounds(left + 8, bottom, 34, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                button -> onClose()).bounds(left + panelWidth / 2 - 38, bottom, 76, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            int max = Math.max(0, (this.filteredItems.size() - 1) / PAGE_SIZE);
            this.page = Math.min(max, this.page + 1);
            rebuild();
        }).bounds(left + panelWidth - 42, bottom, 34, 18).build());
        this.setInitialFocus(this.searchBox);
        this.searchBox.setCursorPosition(this.query.length());
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
        if (!this.rebuildQueued) {
            this.rebuildQueued = true;
            Minecraft.getInstance().execute(() -> {
                this.rebuildQueued = false;
                rebuild();
            });
        }
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, (this.filteredItems.size() - 1) / PAGE_SIZE);
        this.page = Math.max(0, Math.min(max, this.page + (delta < 0 ? 1 : -1)));
        rebuild();
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelWidth = Math.min(300, this.width - 12);
        int panelHeight = Math.min(230, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xf0171a21);
        graphics.drawString(this.font, this.title, left + 8, top + 9, 0xfff0c66a, false);
        int max = Math.max(0, (this.filteredItems.size() - 1) / PAGE_SIZE);
        graphics.drawCenteredString(this.font, (this.page + 1) + "/" + (max + 1),
                left + panelWidth / 2, top + panelHeight - 17, 0xffaeb6c6);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }
}
