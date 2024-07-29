package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * This listener differs
 * from {@link SingleCascadingUpdateShadowVariableListener} because it has a planning list variable as a source.
 * <p>
 * The listener might update only one planning list variable
 * since the targetVariableDescriptorList contains a single field.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SingleCascadingUpdateListVariableListener<Solution_> extends SingleCascadingUpdateShadowVariableListener<Solution_>
        implements ListVariableListener<Solution_, Object, Object> {

    SingleCascadingUpdateListVariableListener(ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor targetMethod,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        super(sourceListVariableDescriptor, targetVariableDescriptorList, targetMethod, listVariableStateSupply);
    }

    @Override
    public void afterListVariableElementUnassigned(ScoreDirector<Solution_> scoreDirector, Object element) {
        // We let the user logic handle the update when the element is unassigned
        execute(scoreDirector, element);
    }

    @Override
    public void beforeListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity, int fromIndex, int toIndex) {
        // Do nothing
    }

    @Override
    public void afterListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity, int fromIndex, int toIndex) {
        var values = getValues(entity);
        // Update all the elements inside the range
        for (var i = fromIndex; i < toIndex; i++) {
            execute(scoreDirector, values.get(i));
        }
        // Double-check if anything beyond the last element in the range needs to be updated
        if (toIndex < values.size()) {
            for (var i = toIndex; i < values.size(); i++) {
                if (!execute(scoreDirector, values.get(i))) {
                    break;
                }
            }
        }
    }
}
