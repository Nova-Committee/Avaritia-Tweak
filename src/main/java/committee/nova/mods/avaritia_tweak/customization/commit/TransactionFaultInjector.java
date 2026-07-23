package committee.nova.mods.avaritia_tweak.customization.commit;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface TransactionFaultInjector {
    TransactionFaultInjector NONE = (index, target) -> {
    };

    void beforeApply(int index, Path target) throws IOException;
}
