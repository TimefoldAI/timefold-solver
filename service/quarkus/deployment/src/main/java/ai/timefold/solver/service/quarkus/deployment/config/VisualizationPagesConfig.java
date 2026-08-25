package ai.timefold.solver.service.quarkus.deployment.config;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "timefold.model.visualization")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface VisualizationPagesConfig {

    /**
     * The visualization pages that a UI should render for this model.
     */
    List<Page> pages();

    interface Page {

        /**
         * Stable identifier of the page.
         */
        String key();

        /**
         * Icon name for the page. Any icon name from <a href="https://tabler.io/icons">Tabler Icons</a> is valid.
         */
        String icon();

        /**
         * Human-readable label for the page.
         */
        String label();
    }
}
