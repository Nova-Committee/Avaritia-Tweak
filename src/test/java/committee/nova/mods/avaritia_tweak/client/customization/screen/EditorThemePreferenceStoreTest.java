package committee.nova.mods.avaritia_tweak.client.customization.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EditorThemePreferenceStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void missingPreferenceUsesClassicThemeWithoutWarning() {
        EditorThemePreferenceStore store = new EditorThemePreferenceStore(
                this.temporaryDirectory.resolve("visual_editor/ui-theme.txt"));

        EditorThemePreferenceStore.LoadResult result = store.load();

        assertThat(result.style()).isEqualTo(EditorThemeStyle.CLASSIC);
        assertThat(result.warning()).isEmpty();
    }

    @Test
    void everyBuiltInThemeRoundTripsThroughThePreferenceFile() throws IOException {
        Path preference = this.temporaryDirectory.resolve("visual_editor/ui-theme.txt");
        EditorThemePreferenceStore store = new EditorThemePreferenceStore(preference);

        for (EditorThemeStyle style : EditorThemeStyle.values()) {
            assertThat(store.save(style)).isEmpty();
            assertThat(store.load().style()).isEqualTo(style);
            assertThat(Files.readString(preference, StandardCharsets.UTF_8)).isEqualTo(style.id() + "\n");
        }
    }

    @Test
    void unknownThemeFallsBackToClassicAndReportsWarning() throws IOException {
        Path preference = this.temporaryDirectory.resolve("ui-theme.txt");
        Files.writeString(preference, "unregistered-theme\n", StandardCharsets.UTF_8);

        EditorThemePreferenceStore.LoadResult result = new EditorThemePreferenceStore(preference).load();

        assertThat(result.style()).isEqualTo(EditorThemeStyle.CLASSIC);
        assertThat(result.warning()).hasValueSatisfying(message ->
                assertThat(message).contains("unregistered-theme"));
    }

    @Test
    void themeOrderCyclesBackToClassic() {
        assertThat(EditorThemeStyle.CLASSIC.next()).isEqualTo(EditorThemeStyle.SAKURA_NIGHT);
        assertThat(EditorThemeStyle.SAKURA_NIGHT.next()).isEqualTo(EditorThemeStyle.VOID);
        assertThat(EditorThemeStyle.VOID.next()).isEqualTo(EditorThemeStyle.CLASSIC);
    }
}
