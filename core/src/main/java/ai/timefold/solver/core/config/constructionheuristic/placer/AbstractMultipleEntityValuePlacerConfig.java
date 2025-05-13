package ai.timefold.solver.core.config.constructionheuristic.placer;

import java.util.List;

/**
 * General superclass for {@link CartesianProductQueuedMultipleEntityValuePlacerConfig} and
 * {@link QueuedMultipleEntityValuePlacerConfig}.
 */
public abstract class AbstractMultipleEntityValuePlacerConfig<Config_ extends AbstractMultipleEntityValuePlacerConfig<Config_>>
        extends EntityPlacerConfig<Config_> {

    public abstract List<EntityPlacerConfig> getPlacerConfigList();
}
