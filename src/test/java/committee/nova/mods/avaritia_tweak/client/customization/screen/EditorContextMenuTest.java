package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EditorContextMenuTest {
    @Test
    void enterRunsFirstEnabledActionAndClosesMenu() {
        AtomicInteger selected = new AtomicInteger();
        EditorContextMenu menu = menu(List.of(
                EditorContextMenu.action(Component.literal("Disabled"), () -> selected.set(1), false),
                EditorContextMenu.action(Component.literal("Open"), () -> selected.set(2))));

        assertThat(menu.keyPressed(GLFW.GLFW_KEY_ENTER)).isTrue();
        assertThat(selected.get()).isEqualTo(2);
        assertThat(menu.isOpen()).isFalse();
    }

    @Test
    void arrowNavigationSkipsDisabledItems() {
        AtomicInteger selected = new AtomicInteger();
        EditorContextMenu menu = menu(List.of(
                EditorContextMenu.action(Component.literal("Open"), () -> selected.set(1)),
                EditorContextMenu.action(Component.literal("Disabled"), () -> selected.set(2), false),
                EditorContextMenu.action(Component.literal("Delete"), () -> selected.set(3))));

        assertThat(menu.keyPressed(GLFW.GLFW_KEY_DOWN)).isTrue();
        assertThat(menu.keyPressed(GLFW.GLFW_KEY_ENTER)).isTrue();
        assertThat(selected.get()).isEqualTo(3);
    }

    @Test
    void escapeClosesWithoutRunningAnAction() {
        AtomicInteger selected = new AtomicInteger();
        EditorContextMenu menu = menu(List.of(
                EditorContextMenu.action(Component.literal("Open"), () -> selected.incrementAndGet())));

        assertThat(menu.keyPressed(GLFW.GLFW_KEY_ESCAPE)).isTrue();
        assertThat(selected.get()).isZero();
        assertThat(menu.isOpen()).isFalse();
    }

    private static EditorContextMenu menu(List<EditorContextMenu.Item> items) {
        Font font = mock(Font.class);
        when(font.width(any(Component.class))).thenReturn(48);
        return EditorContextMenu.open(font, 320, 240, 40, 50, items);
    }
}
