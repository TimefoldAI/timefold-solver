package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.CascadingUpdateCommand;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.Pair;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractCascadingUpdateShadowVariableListener<Solution_> implements VariableListener<Solution_, Object> {

    private final CascadingUpdateCommand<Pair<Integer, Object>> indexElementCommand;
    final ListVariableDescriptor<Solution_> sourceListVariableDescriptor;
    final List<VariableDescriptor<Solution_>> targetVariableDescriptorList;
    final MemberAccessor targetMethod;

    AbstractCascadingUpdateShadowVariableListener(ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor targetMethod,
            CascadingUpdateCommand<Pair<Integer, Object>> indexElementCommand) {
        this.sourceListVariableDescriptor = sourceListVariableDescriptor;
        this.targetVariableDescriptorList = targetVariableDescriptorList;
        this.targetMethod = targetMethod;
        this.indexElementCommand = indexElementCommand;
    }

    abstract boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity);

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        boolean isChanged = execute(scoreDirector, entity);
        if (isChanged) {
            var indexElement = indexElementCommand.getValue(entity);
            if (indexElement != null) {
                int fromIndex = indexElement.firstValue();
                List<Object> values = sourceListVariableDescriptor.getValue(indexElement.secondValue());
                for (int i = fromIndex + 1; i < values.size(); i++) {
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
