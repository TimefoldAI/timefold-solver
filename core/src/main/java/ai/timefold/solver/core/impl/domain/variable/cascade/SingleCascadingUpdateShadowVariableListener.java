package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * The primary listener relies on the user-defined next-element shadow variable
 * to fetch the next element of a given planning value.
 *
 * The listener might update only one shadow variables since the targetVariableDescriptorList contains a single field.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SingleCascadingUpdateShadowVariableListener<Solution_>
        extends AbstractSingleAbstractCascadingUpdateShadowVariableListener<Solution_> {

    private final ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;

    public SingleCascadingUpdateShadowVariableListener(List<VariableDescriptor<Solution_>> targetVariableDescriptorList,
            ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor, MemberAccessor targetMethod) {
        super(targetVariableDescriptorList, targetMethod);
        this.nextElementShadowVariableDescriptor = nextElementShadowVariableDescriptor;
    }

    @Override
    Object getNextElement(Object entity) {
        return nextElementShadowVariableDescriptor.getValue(entity);
    }
}
