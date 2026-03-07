package ai.timefold.solver.migration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;

public final class NoWildCardImportStyle extends NamedStyles {

    public NoWildCardImportStyle() {
        super(UUID.randomUUID(), "ImportStyle", "ImportStyle", "ImportStyle", Collections.emptySet(),
                List.of(ImportLayoutStyle.builder().classCountToUseStarImport(9999999).importStaticAllOthers()
                        .importAllOthers().build()));
    }
}
