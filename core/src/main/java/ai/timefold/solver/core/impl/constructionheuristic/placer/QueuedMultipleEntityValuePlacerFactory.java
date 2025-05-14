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
        // When multiple variable types are used,
        // it is essential to test all values and entities
        // before selecting a specific value and its corresponding destination entity.
        // Therefore, we need to ensure that the QueuedValuePlacer is finite.
        // Otherwise, we may end up streaming unassigned values indefinitely, as the iterator would never finish.
        var sequentialSelection = config instanceof QueuedMultipleEntityValuePlacerConfig;
        var updatedConfigPolicy = configPolicy.createQueuedPlacerConfigPolicy(!sequentialSelection);
        for (var placerConfig : config.getPlacerConfigList()) {
            var placer = EntityPlacerFactory.<Solution_> create(placerConfig).buildEntityPlacer(updatedConfigPolicy);
            queuedPlacerList.add(placer);
        }
        return new QueuedMultipleEntityValuePlacer<>(this, configPolicy, queuedPlacerList, sequentialSelection);
    }
}
