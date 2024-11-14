package ai.timefold.solver.core.impl.domain.variable;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class InternalIndexVariableProcessor<Solution_>
        implements IndexVariableProcessor<Solution_> {

    private final Function<Object, Integer> indexFunction;

    public InternalIndexVariableProcessor(Function<Object, Integer> indexFunction) {
        this.indexFunction = indexFunction;
    }

    @Override
    public void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        // Do nothing.
    }

    @Override
    public void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        // Do nothing.
    }

    @Override
    public void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        // Do nothing.
    }

    @Override
    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        // Do nothing.
    }

    @Override
    public Integer getIndex(Object planningValue) {
        return indexFunction.apply(planningValue);
    }
}
