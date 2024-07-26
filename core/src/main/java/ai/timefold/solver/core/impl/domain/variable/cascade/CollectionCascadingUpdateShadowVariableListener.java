package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.CascadingUpdateCommand;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * Alternative to {@link SingleCascadingUpdateShadowVariableListener}.
 *
 * The listener might update multiple shadow variables since the targetVariableDescriptorList contains various fields.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class CollectionCascadingUpdateShadowVariableListener<Solution_>
        extends AbstractCascadingUpdateShadowVariableListener<Solution_> {

    protected CollectionCascadingUpdateShadowVariableListener(
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor targetMethod,
            CascadingUpdateCommand<Object> nextElementCommand) {
        super(targetVariableDescriptorList, targetMethod, nextElementCommand);
    }

    @Override
    boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var oldValueList = new ArrayList<>(targetVariableDescriptorList.size());
        for (VariableDescriptor<Solution_> targetVariableDescriptor : targetVariableDescriptorList) {
            scoreDirector.beforeVariableChanged(entity, targetVariableDescriptor.getVariableName());
            oldValueList.add(targetVariableDescriptor.getValue(entity));
        }
        targetMethod.executeGetter(entity);
        var newValueList = new ArrayList<>(targetVariableDescriptorList.size());
        var hasChange = false;
        for (int i = 0; i < targetVariableDescriptorList.size(); i++) {
            var targetVariableDescriptor = targetVariableDescriptorList.get(i);
            newValueList.add(targetVariableDescriptor.getValue(entity));
            scoreDirector.afterVariableChanged(entity, targetVariableDescriptor.getVariableName());
            if (!hasChange && !Objects.equals(oldValueList.get(i), newValueList.get(i))) {
                hasChange = true;
            }
        }
        return hasChange;
    }
}
