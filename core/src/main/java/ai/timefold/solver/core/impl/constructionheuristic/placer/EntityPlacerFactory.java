package ai.timefold.solver.core.impl.constructionheuristic.placer;

import ai.timefold.solver.core.config.constructionheuristic.placer.AbstractMultipleEntityValuePlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.CartesianProductQueuedMultipleEntityValuePlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.PooledEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedMultipleEntityValuePlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;

public interface EntityPlacerFactory<Solution_> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <Solution_> EntityPlacerFactory<Solution_> create(EntityPlacerConfig<?> entityPlacerConfig) {
        if (entityPlacerConfig instanceof PooledEntityPlacerConfig pooledEntityPlacerConfig) {
            return new PooledEntityPlacerFactory<>(pooledEntityPlacerConfig);
        } else if (entityPlacerConfig instanceof QueuedEntityPlacerConfig queuedEntityPlacerConfig) {
            return new QueuedEntityPlacerFactory<>(queuedEntityPlacerConfig);
        } else if (entityPlacerConfig instanceof QueuedValuePlacerConfig queuedValuePlacerConfig) {
            return new QueuedValuePlacerFactory<>(queuedValuePlacerConfig);
        } else if (entityPlacerConfig instanceof CartesianProductQueuedMultipleEntityValuePlacerConfig
                || entityPlacerConfig instanceof QueuedMultipleEntityValuePlacerConfig) {
            return new QueuedMultipleEntityValuePlacerFactory<>((AbstractMultipleEntityValuePlacerConfig) entityPlacerConfig);
        } else {
            throw new IllegalArgumentException(String.format("Unknown %s type: (%s).",
                    EntityPlacerConfig.class.getSimpleName(), entityPlacerConfig.getClass().getName()));
        }
    }

    EntityPlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy);
}
