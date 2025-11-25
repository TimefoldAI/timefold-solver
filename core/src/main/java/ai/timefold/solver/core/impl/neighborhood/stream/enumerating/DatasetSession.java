package ai.timefold.solver.core.impl.neighborhood.stream.enumerating;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.AbstractSession;
import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractRightDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractRightDatasetInstance;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DatasetSession<Solution_> extends AbstractSession {

    private final Map<AbstractDataset<Solution_>, AbstractDatasetInstance<Solution_, ?>> leftDatasetInstanceMap =
            new IdentityHashMap<>();
    private final Map<AbstractDataset<Solution_>, AbstractDatasetInstance<Solution_, ?>> rightDatasetInstanceMap =
            new IdentityHashMap<>();

    DatasetSession(NodeNetwork nodeNetwork) {
        super(nodeNetwork);
    }

    public void registerDatasetInstance(AbstractDataset<Solution_> dataset,
            AbstractDatasetInstance<Solution_, ?> datasetInstance) {
        var map = datasetInstance instanceof AbstractLeftDatasetInstance ? leftDatasetInstanceMap : rightDatasetInstanceMap;
        var oldDatasetInstance = map.put(dataset, datasetInstance);
        if (oldDatasetInstance != null) {
            throw new IllegalStateException("The dataset (%s) has already been registered with session (%s)."
                    .formatted(dataset, this));
        }
    }

    @SuppressWarnings("unchecked")
    public <A> AbstractLeftDatasetInstance<Solution_, UniTuple<A>> getInstance(AbstractLeftDataset<Solution_, A> dataset) {
        return (AbstractLeftDatasetInstance<Solution_, UniTuple<A>>) Objects
                .requireNonNull(leftDatasetInstanceMap.get(dataset));
    }

    @SuppressWarnings("unchecked")
    public <B> AbstractRightDatasetInstance<Solution_, B> getInstance(AbstractRightDataset<Solution_, B> dataset) {
        return (AbstractRightDatasetInstance<Solution_, B>) Objects.requireNonNull(rightDatasetInstanceMap.get(dataset));
    }

}
