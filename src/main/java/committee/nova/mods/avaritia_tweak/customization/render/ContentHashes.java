package committee.nova.mods.avaritia_tweak.customization.render;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ContentHashes {
    private ContentHashes() {
    }

    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static String sha256(String content) {
        return sha256(content.getBytes(StandardCharsets.UTF_8));
    }
}
