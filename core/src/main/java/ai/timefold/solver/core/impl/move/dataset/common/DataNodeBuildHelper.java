package ai.timefold.solver.core.impl.move.dataset.common;

import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.move.dataset.AbstractDataStream;

public final class DataNodeBuildHelper<Solution_> extends AbstractNodeBuildHelper<AbstractDataStream<Solution_>> {

    public DataNodeBuildHelper(Set<AbstractDataStream<Solution_>> activeStreamSet) {
        super(activeStreamSet);
    }

}
