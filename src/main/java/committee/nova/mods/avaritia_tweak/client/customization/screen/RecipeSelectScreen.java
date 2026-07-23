package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.importers.AvaritiaRecipeImporter;
import committee.nova.mods.avaritia_tweak.client.customization.importers.RecipeImportResult;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
    private static final int PAGE_SIZE = 9;
    private final Screen previous;
    private final OutputTarget target;
    private final Consumer<RecipeImportResult> onImported;
    private final AvaritiaRecipeImporter importer = new AvaritiaRecipeImporter();
    private final List<Recipe<?>> allRecipes = new ArrayList<>();
    private final List<Recipe<?>> filteredRecipes = new ArrayList<>();
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
        int panelWidth = Math.min(380, this.width - 12);
        int panelHeight = Math.min(224, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int listWidth = Math.max(150, panelWidth - 144);
        int detailX = left + listWidth + 8;
        int detailWidth = panelWidth - listWidth - 16;
        this.searchBox = new EditBox(this.font, left + 8, top + 8, listWidth - 16, 20,
                Component.translatable("gui.avaritia_tweak.search"));
        this.searchBox.setValue(this.query);
        this.searchBox.setResponder(this::filter);
        this.addRenderableWidget(this.searchBox);
        addRecipeButtons(left + 8, top + 36, listWidth - 16);
        int bottom = top + panelHeight - 22;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            rebuild();
        }).bounds(left + 8, bottom, 34, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            int max = Math.max(0, (this.filteredRecipes.size() - 1) / PAGE_SIZE);
            this.page = Math.min(max, this.page + 1);
            rebuild();
        }).bounds(left + listWidth - 42, bottom, 34, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.import_recipe"),
                button -> importSelected()).bounds(detailX, top + panelHeight - 58,
                detailWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                button -> onClose()).bounds(detailX, top + panelHeight - 30,
                detailWidth, 20).build());
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
                rebuild();
            });
        }
    }

    private void addRecipeButtons(int x, int y, int width) {
        int start = this.page * PAGE_SIZE;
        for (int index = start; index < Math.min(this.filteredRecipes.size(), start + PAGE_SIZE); index++) {
            Recipe<?> recipe = this.filteredRecipes.get(index);
            String id = recipe.getId().toString();
            this.addRenderableWidget(Button.builder(Component.literal(
                            ScreenText.ellipsize(id, Math.max(12, width / 6))), button -> this.selected = recipe)
                    .bounds(x, y + (index - start) * 18, width, 17).build());
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
        int max = Math.max(0, (this.filteredRecipes.size() - 1) / PAGE_SIZE);
        this.page = Math.max(0, Math.min(max, this.page + (delta < 0 ? 1 : -1)));
        rebuild();
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelWidth = Math.min(380, this.width - 12);
        int panelHeight = Math.min(224, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int listWidth = Math.max(150, panelWidth - 144);
        int detailX = left + listWidth + 8;
        int detailWidth = panelWidth - listWidth - 16;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xee171a21);
        graphics.drawString(this.font, this.title, detailX, top + 12, 0xfff0c66a, false);
        int max = Math.max(0, (this.filteredRecipes.size() - 1) / PAGE_SIZE);
        graphics.drawCenteredString(this.font, (this.page + 1) + "/" + (max + 1),
                left + listWidth / 2, top + panelHeight - 17, 0xffaeb6c6);
        if (this.selected != null) {
            ItemStack result = result(this.selected);
            graphics.renderItem(result, detailX, top + 44);
            graphics.drawString(this.font,
                    ScreenText.ellipsize(result.getHoverName().getString(),
                            Math.max(8, detailWidth / 6 - 3)),
                    detailX + 22, top + 48, 0xffffffff, false);
            graphics.drawWordWrap(this.font, Component.literal(this.selected.getId().toString()),
                    detailX, top + 72, detailWidth, 0xffaeb6c6);
            graphics.drawWordWrap(this.font,
                    Component.literal(this.selected.getClass().getSimpleName()),
                    detailX, top + 104, detailWidth, 0xff78d6a3);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

}
