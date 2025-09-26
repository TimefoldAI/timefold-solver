package ai.timefold.solver.core.impl.neighborhood.stream.enumerating;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.AbstractSession;
import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDatasetInstance;

public final class DatasetSession<Solution_> extends AbstractSession {

    private final Map<AbstractDataset<Solution_, ?>, AbstractDatasetInstance<Solution_, ?>> datasetInstanceMap =
            new IdentityHashMap<>();

    DatasetSession(NodeNetwork nodeNetwork) {
        super(nodeNetwork);
    }

    public void registerDatasetInstance(AbstractDataset<Solution_, ?> dataset,
            AbstractDatasetInstance<Solution_, ?> datasetInstance) {
        var oldDatasetInstance = datasetInstanceMap.put(dataset, datasetInstance);
        if (oldDatasetInstance != null) {
            throw new IllegalStateException("The dataset (%s) has already been registered with session (%s)."
                    .formatted(dataset, this));
        }
    }

    @SuppressWarnings("unchecked")
    public <Out_ extends AbstractTuple> AbstractDatasetInstance<Solution_, Out_>
            getInstance(AbstractDataset<Solution_, Out_> dataset) {
        return (AbstractDatasetInstance<Solution_, Out_>) Objects.requireNonNull(datasetInstanceMap.get(dataset));
    }

}
