package ai.timefold.solver.core.impl.domain.variable.descriptor;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;

import org.jspecify.annotations.Nullable;

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
        super(ordinal, entityDescriptor, variableMemberAccessor, true);
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
     * @return if null, there is no source variable
     */
    public @Nullable VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return null;
    }

    /**
     * @return never null, the classes responsible for updating this shadow variable
     */
    public abstract Collection<Class<?>> getUpdaterClasses();

    /**
     * @return never null
     */
    public abstract Demand<?> getProvidedDemand();

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    @Override
    public String toString() {
        return getSimpleEntityAndVariableName() + " shadow";
    }

}
