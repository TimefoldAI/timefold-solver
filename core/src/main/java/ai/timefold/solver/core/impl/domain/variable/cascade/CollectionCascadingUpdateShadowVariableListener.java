package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
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

    private final List<VariableDescriptor<Solution_>> targetDescriptorList;

    protected CollectionCascadingUpdateShadowVariableListener(ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor targetMethod,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        super(sourceListVariableDescriptor, targetMethod, listVariableStateSupply);
        this.targetDescriptorList = targetVariableDescriptorList;
    }

    @Override
    protected boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var oldValueList = new ArrayList<>(targetDescriptorList.size());
        for (var targetVariableDescriptor : targetDescriptorList) {
            scoreDirector.beforeVariableChanged(entity, targetVariableDescriptor.getVariableName());
            oldValueList.add(targetVariableDescriptor.getValue(entity));
        }
        runTargetMethod(entity);
        var hasChange = false;
        for (var i = 0; i < targetDescriptorList.size(); i++) {
            var targetVariableDescriptor = targetDescriptorList.get(i);
            var newValue = targetVariableDescriptor.getValue(entity);
            scoreDirector.afterVariableChanged(entity, targetVariableDescriptor.getVariableName());
            if (!hasChange && !Objects.equals(oldValueList.get(i), newValue)) {
                hasChange = true;
            }
        }
        return hasChange;
    }
}
