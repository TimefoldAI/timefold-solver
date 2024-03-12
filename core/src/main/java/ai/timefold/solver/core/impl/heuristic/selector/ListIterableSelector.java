package ai.timefold.solver.core.impl.heuristic.selector;

import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.ListIterable;

public interface ListIterableSelector<Solution_, T> extends IterableSelector<Solution_, T>, ListIterable<T> {

}
