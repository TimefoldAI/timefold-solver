package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractCascadingUpdateShadowVariableListener<Solution_> implements VariableListener<Solution_, Object> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final ListVariableDescriptor<Solution_> sourceListVariableDescriptor;
    private final MemberAccessor targetMethod;

    AbstractCascadingUpdateShadowVariableListener(ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            MemberAccessor targetMethod, ListVariableStateSupply<Solution_> listVariableStateSupply) {
        this.sourceListVariableDescriptor = sourceListVariableDescriptor;
        this.targetMethod = targetMethod;
        this.listVariableStateSupply = listVariableStateSupply;
    }

    abstract boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity);

    void runTargetMethod(Object entity) {
        targetMethod.executeGetter(entity);
    }

    protected List<Object> getValues(Object entity) {
        return sourceListVariableDescriptor.getValue(entity);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var isChanged = execute(scoreDirector, entity);
        if (isChanged) {
            var indexElement = listVariableStateSupply.getLocationInList(entity);
            if (indexElement instanceof LocationInList location) {
                var fromIndex = location.index();
                List<Object> values = getValues(location.entity());
                for (var i = fromIndex + 1; i < values.size(); i++) {
                    if (!execute(scoreDirector, values.get(i))) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

}
