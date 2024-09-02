package ai.timefold.solver.core.impl.domain.variable.descriptor;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.MovableChainedTrailingValueFilter;

public final class BasicVariableDescriptor<Solution_> extends GenuineVariableDescriptor<Solution_> {

    private SelectionFilter<Solution_, Object> movableChainedTrailingValueFilter;
    private boolean chained;
    private boolean allowsUnassigned;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public BasicVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    public boolean isChained() {
        return chained;
    }

    public boolean allowsUnassigned() {
        return allowsUnassigned;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    protected void processPropertyAnnotations(DescriptorPolicy descriptorPolicy) {
        PlanningVariable planningVariableAnnotation = variableMemberAccessor.getAnnotation(PlanningVariable.class);
        processAllowsUnassigned(planningVariableAnnotation);
        processChained(planningVariableAnnotation);
        processValueRangeRefs(descriptorPolicy, planningVariableAnnotation.valueRangeProviderRefs());
        processStrength(planningVariableAnnotation.strengthComparatorClass(),
                planningVariableAnnotation.strengthWeightFactoryClass());
    }

    private void processAllowsUnassigned(PlanningVariable planningVariableAnnotation) {
        var deprecatedNullable = planningVariableAnnotation.nullable();
        if (planningVariableAnnotation.allowsUnassigned()) {
            // If the user has specified allowsUnassigned = true, it takes precedence.
            if (deprecatedNullable) {
                throw new IllegalArgumentException(
                        "The entityClass (%s) has a @%s-annotated property (%s) with allowsUnassigned (%s) and nullable (%s) which are mutually exclusive."
                                .formatted(entityDescriptor.getEntityClass(), PlanningVariable.class.getSimpleName(),
                                        variableMemberAccessor.getName(), true, true));
            }
            this.allowsUnassigned = true;
        } else { // If the user has not specified allowsUnassigned = true, nullable is taken.
            this.allowsUnassigned = deprecatedNullable;
        }
        if (this.allowsUnassigned && variableMemberAccessor.getType().isPrimitive()) {
            throw new IllegalArgumentException(
                    "The entityClass (%s) has a @%s-annotated property (%s) with allowsUnassigned (%s) which is not compatible with the primitive propertyType (%s)."
                            .formatted(entityDescriptor.getEntityClass(),
                                    PlanningVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    this.allowsUnassigned,
                                    variableMemberAccessor.getType()));
        }
    }

    private void processChained(PlanningVariable planningVariableAnnotation) {
        chained = planningVariableAnnotation.graphType() == PlanningVariableGraphType.CHAINED;
        if (!chained) {
            return;
        }
        if (!acceptsValueType(entityDescriptor.getEntityClass())) {
            throw new IllegalArgumentException(
                    """
                            The entityClass (%s) has a @%s-annotated property (%s) with chained (%s) and propertyType (%s) which is not a superclass/interface of or the same as the entityClass (%s).
                            If an entity's chained planning variable cannot point to another entity of the same class, then it is impossible to make a chain longer than 1 entity and therefore chaining is useless."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    PlanningVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    chained,
                                    getVariablePropertyType(),
                                    entityDescriptor.getEntityClass()));
        }
        if (allowsUnassigned) {
            throw new IllegalArgumentException(
                    "The entityClass (%s) has a @%s-annotated property (%s) with chained (%s), which is not compatible with nullable (%s)."
                            .formatted(entityDescriptor.getEntityClass(),
                                    PlanningVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    chained,
                                    allowsUnassigned));
        }
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        super.linkVariableDescriptors(descriptorPolicy);
        if (chained && entityDescriptor.hasEffectiveMovableEntityFilter()) {
            movableChainedTrailingValueFilter = new MovableChainedTrailingValueFilter<>(this);
        } else {
            movableChainedTrailingValueFilter = null;
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean acceptsValueType(Class<?> valueType) {
        return getVariablePropertyType().isAssignableFrom(valueType);
    }

    @Override
    public boolean isInitialized(Object entity) {
        return allowsUnassigned || getValue(entity) != null;
    }

    public boolean hasMovableChainedTrailingValueFilter() {
        return movableChainedTrailingValueFilter != null;
    }

    public SelectionFilter<Solution_, Object> getMovableChainedTrailingValueFilter() {
        return movableChainedTrailingValueFilter;
    }

}
