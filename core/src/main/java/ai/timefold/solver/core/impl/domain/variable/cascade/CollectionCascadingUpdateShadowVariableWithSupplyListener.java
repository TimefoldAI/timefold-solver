package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementVariableSupply;

/**
 * Alternative to {@link CollectionCascadingUpdateShadowVariableListener} when there is no user-defined
 * {@link ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable}.
 *
 * The listener might update multiple shadow variables since the targetVariableDescriptorList contains various fields.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class CollectionCascadingUpdateShadowVariableWithSupplyListener<Solution_>
        extends AbstractCollectionAbstractCascadingUpdateShadowVariableListener<Solution_> {

    private final NextElementVariableSupply nextElementVariableSupply;

    public CollectionCascadingUpdateShadowVariableWithSupplyListener(
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList,
            NextElementVariableSupply nextElementVariableSupply, MemberAccessor targetMethod) {
        super(targetVariableDescriptorList, targetMethod);
        this.nextElementVariableSupply = nextElementVariableSupply;
    }

    @Override
    Object getNextElement(Object entity) {
        return nextElementVariableSupply.getNext(entity);
    }
}
