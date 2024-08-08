package ai.timefold.solver.core.impl.domain.variable.descriptor;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class ShadowVariableDescriptor<Solution_> extends VariableDescriptor<Solution_> {

    private int globalShadowOrder = Integer.MAX_VALUE;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    protected ShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    public int getGlobalShadowOrder() {
        return globalShadowOrder;
    }

    public void setGlobalShadowOrder(int globalShadowOrder) {
        this.globalShadowOrder = globalShadowOrder;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public abstract void processAnnotations(DescriptorPolicy descriptorPolicy);

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * Inverse of {@link #getSinkVariableDescriptorList()}.
     *
     * @return never null, only variables affect this shadow variable directly
     */
    public abstract List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList();

    public abstract Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses();

    /**
     * @return never null
     */
    public abstract Demand<?> getProvidedDemand();

    public boolean hasVariableListener() {
        return true;
    }

    /**
     * @param supplyManager never null
     * @return never null
     */
    public abstract Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager);

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    @Override
    public String toString() {
        return getSimpleEntityAndVariableName() + " shadow";
    }

}
