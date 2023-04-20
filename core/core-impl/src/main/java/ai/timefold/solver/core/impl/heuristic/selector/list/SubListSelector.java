package ai.timefold.solver.core.impl.heuristic.selector.list;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.IterableSelector;

public interface SubListSelector<Solution_> extends IterableSelector<Solution_, SubList> {

    ListVariableDescriptor<Solution_> getVariableDescriptor();

    Iterator<Object> endingValueIterator();

    long getValueCount();
}
