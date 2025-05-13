package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;

import ai.timefold.solver.core.config.constructionheuristic.placer.AbstractMultipleEntityValuePlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedMultipleEntityValuePlacerConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;

public final class QueuedMultipleEntityValuePlacerFactory<Solution_, EntityPlacerConfig_ extends AbstractMultipleEntityValuePlacerConfig<EntityPlacerConfig_>>
        extends AbstractEntityPlacerFactory<Solution_, EntityPlacerConfig_> {

    public QueuedMultipleEntityValuePlacerFactory(EntityPlacerConfig_ config) {
        super(config);
    }

    @Override
    public EntityPlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy) {
        var queuedPlacerList = new ArrayList<EntityPlacer<Solution_>>();
        for (var placerConfig : config.getPlacerConfigList()) {
            var placer = EntityPlacerFactory.<Solution_> create(placerConfig).buildEntityPlacer(configPolicy);
            queuedPlacerList.add(placer);
        }
        return new QueuedMultipleEntityValuePlacer<>(this, configPolicy, queuedPlacerList,
                config instanceof QueuedMultipleEntityValuePlacerConfig);
    }
}
