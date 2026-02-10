package ai.timefold.solver.core.impl.domain.variable.descriptor;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

public final class BasicVariableDescriptor<Solution_> extends GenuineVariableDescriptor<Solution_> {

    private boolean allowsUnassigned;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public BasicVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
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
        processValueRangeRefs(descriptorPolicy, planningVariableAnnotation.valueRangeProviderRefs());
        var sortingProperties = assertSortingProperties(planningVariableAnnotation);
        processSorting(sortingProperties.comparatorPropertyName(), sortingProperties.comparatorClass(),
                sortingProperties.comparatorFactoryPropertyName(), sortingProperties.comparatorFactoryClass());
    }

    private SortingProperties assertSortingProperties(PlanningVariable planningVariableAnnotation) {
        // Comparator property
        var strengthComparatorClass = planningVariableAnnotation.strengthComparatorClass();
        var comparatorClass = planningVariableAnnotation.comparatorClass();
        if (strengthComparatorClass != null
                && PlanningVariable.NullComparator.class.isAssignableFrom(strengthComparatorClass)) {
            strengthComparatorClass = null;
        }
        if (comparatorClass != null && PlanningVariable.NullComparator.class.isAssignableFrom(comparatorClass)) {
            comparatorClass = null;
        }
        if (strengthComparatorClass != null && comparatorClass != null) {
            throw new IllegalStateException(
                    "The entityClass (%s) property (%s) cannot have a %s (%s) and a %s (%s) at the same time.".formatted(
                            entityDescriptor.getEntityClass(), variableMemberAccessor.getName(), "strengthComparatorClass",
                            strengthComparatorClass.getName(), "comparatorClass", comparatorClass.getName()));
        }
        // Comparator factory property
        var strengthWeightFactoryClass = planningVariableAnnotation.strengthWeightFactoryClass();
        var comparatorFactoryClass = planningVariableAnnotation.comparatorFactoryClass();
        if (strengthWeightFactoryClass != null
                && PlanningVariable.NullComparatorFactory.class.isAssignableFrom(strengthWeightFactoryClass)) {
            strengthWeightFactoryClass = null;
        }
        if (comparatorFactoryClass != null
                && PlanningVariable.NullComparatorFactory.class.isAssignableFrom(comparatorFactoryClass)) {
            comparatorFactoryClass = null;
        }
        if (strengthWeightFactoryClass != null && comparatorFactoryClass != null) {
            throw new IllegalStateException(
                    "The entityClass (%s) property (%s) cannot have a %s (%s) and a %s (%s) at the same time.".formatted(
                            entityDescriptor.getEntityClass(), variableMemberAccessor.getName(), "strengthWeightFactoryClass",
                            strengthWeightFactoryClass.getName(), "comparatorFactoryClass", comparatorFactoryClass.getName()));
        }
        // Selected settings
        var selectedComparatorPropertyName = "comparatorClass";
        var selectedComparatorClass = comparatorClass;
        var selectedComparatorFactoryPropertyName = "comparatorFactoryClass";
        var selectedComparatorFactoryClass = comparatorFactoryClass;
        if (strengthComparatorClass != null) {
            selectedComparatorPropertyName = "strengthComparatorClass";
            selectedComparatorClass = strengthComparatorClass;
        }
        if (strengthWeightFactoryClass != null) {
            selectedComparatorFactoryPropertyName = "strengthWeightFactoryClass";
            selectedComparatorFactoryClass = strengthWeightFactoryClass;
        }
        return new SortingProperties(selectedComparatorPropertyName, selectedComparatorClass,
                selectedComparatorFactoryPropertyName, selectedComparatorFactoryClass);
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

    @Override
    @SuppressWarnings("unchecked")
    public <Entity_, Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> getVariableMetaModel() {
        return (PlanningVariableMetaModel<Solution_, Entity_, Value_>) super.getVariableMetaModel();
    }

    private record SortingProperties(String comparatorPropertyName, Class<? extends Comparator> comparatorClass,
            String comparatorFactoryPropertyName, Class<? extends ComparatorFactory> comparatorFactoryClass) {

    }

}
