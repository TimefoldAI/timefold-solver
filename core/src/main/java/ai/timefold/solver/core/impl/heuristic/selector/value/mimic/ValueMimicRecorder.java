package ai.timefold.solver.core.impl.heuristic.selector.value.mimic;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;

public interface ValueMimicRecorder<Solution_> {

    /**
     * @param replayingValueSelector never null
     */
    void addMimicReplayingValueSelector(MimicReplayingValueSelector<Solution_> replayingValueSelector);

    /**
     * @return As defined by {@link ValueSelector#getVariableDescriptor()}
     * @see ValueSelector#getVariableDescriptor()
     */
    GenuineVariableDescriptor<Solution_> getVariableDescriptor();

    /**
     * @return As defined by {@link ValueSelector#isNeverEnding()}
     * @see ValueSelector#isNeverEnding()
     */
    boolean isNeverEnding();

    /**
     * @return As defined by {@link IterableValueSelector#getSize()}
     * @see IterableValueSelector#getSize()
     */
    long getSize();

    /**
     * @return As defined by {@link ValueSelector#getSize(Object)}
     * @see ValueSelector#getSize(Object)
     */
    long getSize(Object entity);

    /**
     * @return As defined by {@link ValueSelector#endingIterator(Object)}
     * @see ValueSelector#endingIterator(Object)
     */
    Iterator<Object> endingIterator(Object entity);

}
