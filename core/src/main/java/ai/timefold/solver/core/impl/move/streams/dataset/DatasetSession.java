package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.AbstractSession;
import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

public final class DatasetSession<Solution_> extends AbstractSession {

    private final Map<AbstractDataset<Solution_, ?>, DatasetInstance<Solution_, ?>> datasetInstanceMap =
            new IdentityHashMap<>();

    DatasetSession(NodeNetwork nodeNetwork) {
        super(nodeNetwork);
    }

    public void registerDatasetInstance(AbstractDataset<Solution_, ?> dataset, DatasetInstance<Solution_, ?> datasetInstance) {
        var oldDatasetInstance = datasetInstanceMap.put(dataset, datasetInstance);
        if (oldDatasetInstance != null) {
            throw new IllegalStateException("The dataset (%s) has already been registered with session (%s)."
                    .formatted(dataset, this));
        }
    }

    @SuppressWarnings("unchecked")
    public <Out_ extends AbstractTuple> DatasetInstance<Solution_, Out_> getInstance(AbstractDataset<Solution_, Out_> dataset) {
        return (DatasetInstance<Solution_, Out_>) Objects.requireNonNull(datasetInstanceMap.get(dataset));
    }

}
