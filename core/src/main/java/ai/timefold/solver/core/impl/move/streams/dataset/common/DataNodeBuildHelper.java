package ai.timefold.solver.core.impl.move.streams.dataset.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.director.SessionContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DataNodeBuildHelper<Solution_> extends AbstractNodeBuildHelper<AbstractDataStream<Solution_>> {

    private final SessionContext<Solution_> sessionContext;
    private final List<AbstractDatasetInstance<Solution_, ?>> datasetInstanceList = new ArrayList<>();

    public DataNodeBuildHelper(SessionContext<Solution_> sessionContext, Set<AbstractDataStream<Solution_>> activeStreamSet) {
        super(activeStreamSet);
        this.sessionContext = Objects.requireNonNull(sessionContext);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <Tuple_ extends AbstractTuple> void putInsertUpdateRetract(AbstractDataStream<Solution_> stream,
            TupleLifecycle<Tuple_> tupleLifecycle) {
        super.putInsertUpdateRetract(stream, tupleLifecycle);
        if (tupleLifecycle instanceof AbstractDatasetInstance datasetInstance) {
            datasetInstanceList.add(datasetInstance);
        }
    }

    public SessionContext<Solution_> getSessionContext() {
        return sessionContext;
    }

    public List<AbstractDatasetInstance<Solution_, ?>> getDatasetInstanceList() {
        return Collections.unmodifiableList(datasetInstanceList);
    }
}
