package committee.nova.mods.avaritia_tweak.customization.render;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class NbtText {
    private static final Pattern SIMPLE_KEY = Pattern.compile("[A-Za-z0-9._+\\-]+");

    private NbtText() {
    }

    public static String canonical(CompoundTag tag) {
        return write(tag);
    }

    private static String write(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            Collections.sort(keys);
            StringBuilder result = new StringBuilder("{");
            for (int index = 0; index < keys.size(); index++) {
                if (index > 0) {
                    result.append(',');
                }
                String key = keys.get(index);
                result.append(key(key)).append(':').append(write(compound.get(key)));
            }
            return result.append('}').toString();
        }
        if (tag instanceof ListTag list) {
            StringBuilder result = new StringBuilder("[");
            for (int index = 0; index < list.size(); index++) {
                if (index > 0) {
                    result.append(',');
                }
                result.append(write(list.get(index)));
            }
            return result.append(']').toString();
        }
        if (tag instanceof StringTag string) {
            return quoteSnbt(string.getAsString());
        }
        return tag.toString();
    }

    private static String key(String value) {
        return SIMPLE_KEY.matcher(value).matches() ? value : quoteSnbt(value);
    }

    private static String quoteSnbt(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\\' || character == '"') {
                result.append('\\');
            }
            result.append(character);
        }
        return result.append('"').toString();
    }
}
