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

    private static SortingProperties assertSortingProperties(PlanningVariable planningVariableAnnotation) {
        // Comparator property
        var comparatorClass = planningVariableAnnotation.comparatorClass();
        if (comparatorClass != null && PlanningVariable.NullComparator.class.isAssignableFrom(comparatorClass)) {
            comparatorClass = null;
        }
        // Comparator factory property
        var comparatorFactoryClass = planningVariableAnnotation.comparatorFactoryClass();
        if (comparatorFactoryClass != null
                && PlanningVariable.NullComparatorFactory.class.isAssignableFrom(comparatorFactoryClass)) {
            comparatorFactoryClass = null;
        }
        // Selected settings
        return new SortingProperties("comparatorClass", comparatorClass,
                "comparatorFactoryClass", comparatorFactoryClass);
    }

    private void processAllowsUnassigned(PlanningVariable planningVariableAnnotation) {
        allowsUnassigned = planningVariableAnnotation.allowsUnassigned();
        if (allowsUnassigned && variableMemberAccessor.getType().isPrimitive()) {
            throw new IllegalArgumentException(
                    "The entityClass (%s) has a @%s-annotated property (%s) with allowsUnassigned (%s) which is not compatible with the primitive propertyType (%s)."
                            .formatted(entityDescriptor.getEntityClass(),
                                    PlanningVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    allowsUnassigned,
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
