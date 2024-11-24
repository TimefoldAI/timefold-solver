package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class InternalNextPrevVariableProcessor<Solution_>
        implements NextPrevElementVariableProcessor<Solution_> {

    private final Function<Object, Object> elementFunction;

    public InternalNextPrevVariableProcessor(Function<Object, Object> elementFunction) {
        this.elementFunction = elementFunction;
    }

    @Override
    public void setElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            Integer index) {
        // Do nothing.
    }

    @Override
    public void unsetElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        // Do nothing.
    }

    @Override
    public Object getElement(Object element) {
        return elementFunction.apply(element);
    }
}
