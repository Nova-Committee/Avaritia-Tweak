package committee.nova.mods.avaritia_tweak.client.customization.screen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextTest {
    @Test
    void matchesCommittedImeTextAgainstLocalizedNames() {
        assertThat(SearchText.matches("  无限锭  ", "avaritia:infinity_ingot", "无限锭")).isTrue();
        assertThat(SearchText.matches("中子", "avaritia:neutron", "中子素锭")).isTrue();
        assertThat(SearchText.matches("龙锭", "avaritia:infinity_ingot", "无限锭")).isFalse();
    }

    @Test
    void normalizesCaseAndFullWidthLatinInput() {
        assertThat(SearchText.matches("ＤＩＡＭＯＮＤ", "minecraft:diamond", "Diamond")).isTrue();
        assertThat(SearchText.matches("", "minecraft:stone")).isTrue();
    }
}
