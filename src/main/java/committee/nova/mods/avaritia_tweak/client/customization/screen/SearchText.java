package committee.nova.mods.avaritia_tweak.client.customization.screen;

import java.text.Normalizer;
import java.util.Locale;

/** Normalizes searchable UI text without restricting Unicode input from an IME. */
final class SearchText {
    private SearchText() {
    }

    static boolean matches(String query, String... candidates) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        for (String candidate : candidates) {
            if (candidate != null && normalize(candidate).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}
