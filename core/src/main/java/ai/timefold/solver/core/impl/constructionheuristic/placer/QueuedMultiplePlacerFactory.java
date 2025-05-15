package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;

import ai.timefold.solver.core.impl.constructionheuristic.placer.internal.QueuedMultiplePlacerConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;

public final class QueuedMultiplePlacerFactory<Solution_>
        extends AbstractEntityPlacerFactory<Solution_, QueuedMultiplePlacerConfig> {

    public QueuedMultiplePlacerFactory(QueuedMultiplePlacerConfig config) {
        super(config);
    }

    @Override
    public EntityPlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy) {
        var queuedPlacerList = new ArrayList<EntityPlacer<Solution_>>();
        for (var placerConfig : config.getPlacerConfigList()) {
            var placer = EntityPlacerFactory.<Solution_> create(placerConfig).buildEntityPlacer(configPolicy);
            queuedPlacerList.add(placer);
        }
        return new QueuedMultiplePlacer<>(this, configPolicy, queuedPlacerList);
    }
}
