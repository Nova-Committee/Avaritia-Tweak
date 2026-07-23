package committee.nova.mods.avaritia_tweak.customization.model;

import java.util.List;

/** The single managed artifact family an entry is committed to. */
public enum OutputTarget {
    KUBEJS,
    CRAFTTWEAKER,
    DATAPACK;

    private static final List<OutputTarget> SCRIPT_TARGETS = List.of(KUBEJS, CRAFTTWEAKER);
    private static final List<OutputTarget> DEFINITION_TARGETS = List.of(values());

    public boolean supports(EntryKind kind) {
        return compatibleWith(kind).contains(this);
    }

    public static List<OutputTarget> compatibleWith(EntryKind kind) {
        return kind == EntryKind.SINGULARITY_DEFINITION
                ? DEFINITION_TARGETS
                : SCRIPT_TARGETS;
    }
}
