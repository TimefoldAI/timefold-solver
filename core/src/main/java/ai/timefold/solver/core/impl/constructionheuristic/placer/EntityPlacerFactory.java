package ai.timefold.solver.core.impl.constructionheuristic.placer;

import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.PooledEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;

public interface EntityPlacerFactory<Solution_> {

    static <Solution_> EntityPlacerFactory<Solution_> create(EntityPlacerConfig<?> entityPlacerConfig) {
        if (PooledEntityPlacerConfig.class.isAssignableFrom(entityPlacerConfig.getClass())) {
            return new PooledEntityPlacerFactory<>((PooledEntityPlacerConfig) entityPlacerConfig);
        } else if (QueuedEntityPlacerConfig.class.isAssignableFrom(entityPlacerConfig.getClass())) {
            return new QueuedEntityPlacerFactory<>((QueuedEntityPlacerConfig) entityPlacerConfig);
        } else if (QueuedValuePlacerConfig.class.isAssignableFrom(entityPlacerConfig.getClass())) {
            return new QueuedValuePlacerFactory<>((QueuedValuePlacerConfig) entityPlacerConfig);
        } else {
            throw new IllegalArgumentException(String.format("Unknown %s type: (%s).",
                    EntityPlacerConfig.class.getSimpleName(), entityPlacerConfig.getClass().getName()));
        }
    }

    EntityPlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy);
}
