package ai.timefold.solver.core.impl.domain.variable;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class ExternalizedSingletonListListInverseVariableProcessor<Solution_>
        implements SingletonListInverseVariableProcessor<Solution_> {

    private final Function<Object, Object> inverseSingletonFunction;

    public ExternalizedSingletonListListInverseVariableProcessor(Function<Object, Object> inverseSingletonFunction) {
        this.inverseSingletonFunction = inverseSingletonFunction;
    }

    @Override
    public void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        // Do nothing.
    }

    @Override
    public void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        // Do nothing.
    }

    @Override
    public void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        // Do nothing.
    }

    @Override
    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        // Do nothing.
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        return inverseSingletonFunction.apply(planningValue);
    }
}
