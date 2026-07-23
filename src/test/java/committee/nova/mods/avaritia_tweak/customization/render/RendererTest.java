package committee.nova.mods.avaritia_tweak.customization.render;

import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.SingularityAction;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RendererTest {
    @Test
    void rendersAllSixKubeJsRecipeContractsWithStableIds() {
        String script = new KubeJsRenderer().render(snapshot(recipes(OutputTarget.KUBEJS))).get(0).text();

        assertThat(script)
                .contains("avaritia.shaped_table(", "avaritia.shapeless_table(",
                        "avaritia.compressor(", "avaritia.extreme_smithing(",
                        "avaritia.infinity_catalyst(", "avaritia.eternal_singularity(")
                .contains(".inputCount(1000)", ".timeCost(240)")
                .contains(".id(\"test:shaped\")", ".id(\"test:eternal\")")
                .contains("\"! #\"")
                .doesNotContain("\r")
                .endsWith("\n");
    }

    @Test
    void rendersAllSixCraftTweakerContractsAndActualEnternalSpelling() {
        String script = new CraftTweakerRenderer()
                .render(snapshot(recipes(OutputTarget.CRAFTTWEAKER))).get(0).text();

        assertThat(script)
                .contains("mods.avaritia.CraftingTable.addShaped(",
                        "mods.avaritia.CraftingTable.addShapeless(",
                        "mods.avaritia.Compressor.addRecipe(",
                        "mods.avaritia.ExtremeSmithing.addRecipe(",
                        "mods.avaritia.CraftingTable.addCatalyst(",
                        "mods.avaritia.CraftingTable.addEnternal(")
                .contains("<item:minecraft:air>")
                .doesNotContain("addEternal(")
                .endsWith("\n");
    }

    @Test
    void rendersStrictNbtWithoutWeakeningItsMeaning() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("z", 2);
        nbt.putString("a", "quote \" and slash \\");
        IngredientSpec strict = new IngredientSpec.Item(id("minecraft:stone"), Optional.of(nbt));
        WorkspaceSnapshot kube = snapshot(new CustomizationEntry.Compressor(
                id("test:strict"), OutputTarget.KUBEJS, strict, result(), 1, 1));
        WorkspaceSnapshot craftTweaker = snapshot(new CustomizationEntry.Compressor(
                id("test:strict"), OutputTarget.CRAFTTWEAKER, strict, result(), 1, 1));

        String js = new KubeJsRenderer().render(kube).get(0).text();
        String zs = new CraftTweakerRenderer().render(craftTweaker).get(0).text();

        assertThat(js).contains("Item.of(\"minecraft:stone\"", ").strongNBT()");
        assertThat(zs).contains(".onlyIf(\"avaritia_tweak_strict_", "stack.matches(", ", true)");
        assertThat(NbtText.canonical(nbt)).startsWith("{a:").endsWith(",z:2}");
    }

    @Test
    void rendersSingularityOperationsAndCurrentDatapackFields() {
        CustomizationEntry.SingularityDefinition kubeDefinition = definition(
                "test:kube", OutputTarget.KUBEJS);
        CustomizationEntry.SingularityDefinition dataDefinition = definition(
                "test:data/path", OutputTarget.DATAPACK);
        CustomizationEntry.SingularityOperation removeAll = new CustomizationEntry.SingularityOperation(
                id("test:remove_all"), OutputTarget.KUBEJS, SingularityAction.REMOVE_ALL, Optional.empty());
        CustomizationEntry.SingularityOperation removeRecipe = new CustomizationEntry.SingularityOperation(
                id("test:remove_recipe"), OutputTarget.KUBEJS, SingularityAction.REMOVE_RECIPE,
                Optional.of(id("test:old")));
        WorkspaceSnapshot snapshot = snapshot(kubeDefinition, dataDefinition, removeAll, removeRecipe);

        RenderPlan plan = WorkspaceRenderService.standard().render(snapshot);
        String js = plan.artifacts().get(KubeJsRenderer.PATH).text();
        ArtifactPath dataPath = ArtifactPath.of(SingularityDatapackRenderer.ROOT
                + "data/test/singularities/data/path.json");
        String json = plan.artifacts().get(dataPath).text();

        assertThat(js.indexOf("event.removeAll()")).isLessThan(js.indexOf("event.removeRecipe(\"test:old\")"));
        assertThat(js).contains("event.register(\"test:kube\"", ".setRecipeEnabled(true)");
        assertThat(json)
                .contains("\"name\": \"test:data/path\"", "\"timeCost\"", "\"recipeEnabled\"")
                .doesNotContain("timeRequired", "recipeDisabled");
        assertThat(plan.artifacts()).containsKeys(KubeJsRenderer.PATH, CraftTweakerRenderer.PATH,
                SingularityDatapackRenderer.PACK_METADATA, dataPath);
    }

    @Test
    void renderingTheSameSnapshotIsByteForByteStable() {
        WorkspaceSnapshot snapshot = snapshot(recipes(OutputTarget.KUBEJS));

        RenderPlan first = WorkspaceRenderService.standard().render(snapshot);
        RenderPlan second = WorkspaceRenderService.standard().render(snapshot);

        assertThat(second.artifacts().keySet()).isEqualTo(first.artifacts().keySet());
        first.artifacts().forEach((path, artifact) -> {
            assertThat(second.artifacts().get(path).sha256()).isEqualTo(artifact.sha256());
            assertThat(second.artifacts().get(path).utf8Content()).containsExactly(artifact.utf8Content());
        });
    }

    @Test
    void rejectsUnsafeLogicalPathsAndKeepsResolvedPathsInsideRoot() {
        assertThatThrownBy(() -> ArtifactPath.of("../outside.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArtifactPath.of("safe/../../outside.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArtifactPath.of("C:/outside.txt"))
                .isInstanceOf(IllegalArgumentException.class);

        Path root = Path.of("build", "safe-root").toAbsolutePath().normalize();
        assertThat(ArtifactPath.of("nested/file.txt").resolveWithin(root).toString())
                .startsWith(root.toString());
    }

    private static List<CustomizationEntry> recipes(OutputTarget target) {
        TreeMap<Integer, IngredientSpec> shape = new TreeMap<>();
        shape.put(0, item("minecraft:stone"));
        shape.put(2, tag("forge:ingots/iron"));
        return List.of(
                new CustomizationEntry.ShapedTable(id("test:shaped"), target, CraftingTier.SCULK,
                        shape, result()),
                new CustomizationEntry.ShapelessTable(id("test:shapeless"), target, CraftingTier.NETHER,
                        List.of(item("minecraft:stone"), tag("forge:ingots/iron")), result()),
                new CustomizationEntry.Compressor(id("test:compressor"), target,
                        item("minecraft:stone"), result(), 1000, 240),
                new CustomizationEntry.ExtremeSmithing(id("test:smithing"), target,
                        item("minecraft:stone"), item("minecraft:diamond"),
                        tag("forge:ingots/iron"), result()),
                new CustomizationEntry.InfinityCatalyst(id("test:catalyst"), target, "default",
                        List.of(item("minecraft:stone")), 2),
                new CustomizationEntry.EternalSingularity(id("test:eternal"), target,
                        List.of(item("minecraft:diamond")), 3));
    }

    private static CustomizationEntry.SingularityDefinition definition(String id, OutputTarget target) {
        return new CustomizationEntry.SingularityDefinition(id(id), target, "singularity." + id,
                0x001122, 0xaabbcc, 1000, 240, item("minecraft:stone"), true, true);
    }

    private static WorkspaceSnapshot snapshot(CustomizationEntry... entries) {
        return snapshot(Arrays.asList(entries));
    }

    private static WorkspaceSnapshot snapshot(List<CustomizationEntry> entries) {
        TreeMap<EntryKey, CustomizationEntry> indexed = new TreeMap<>();
        entries.forEach(entry -> indexed.put(entry.key(), entry));
        return new WorkspaceSnapshot(WorkspaceSnapshot.CURRENT_SCHEMA_VERSION, indexed);
    }

    private static IngredientSpec.Item item(String value) {
        return new IngredientSpec.Item(id(value));
    }

    private static IngredientSpec.Tag tag(String value) {
        return new IngredientSpec.Tag(id(value));
    }

    private static ItemStackSpec result() {
        return new ItemStackSpec(id("minecraft:diamond"), 2);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
