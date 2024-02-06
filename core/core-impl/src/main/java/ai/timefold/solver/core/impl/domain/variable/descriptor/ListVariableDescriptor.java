package ai.timefold.solver.core.impl.domain.variable.descriptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.ListVariableDataDemand;

public final class ListVariableDescriptor<Solution_> extends GenuineVariableDescriptor<Solution_> {

    private final ListVariableDataDemand<Solution_> providedDemand = new ListVariableDataDemand<>(this);
    boolean allowsUnassignedValues = true;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ListVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    protected void processPropertyAnnotations(DescriptorPolicy descriptorPolicy) {
        PlanningListVariable planningVariableAnnotation = variableMemberAccessor.getAnnotation(PlanningListVariable.class);
        allowsUnassignedValues = planningVariableAnnotation.allowsUnassignedValues();
        processValueRangeRefs(descriptorPolicy, planningVariableAnnotation.valueRangeProviderRefs());
    }

    @Override
    protected void processValueRangeRefs(DescriptorPolicy descriptorPolicy, String[] valueRangeProviderRefs) {
        List<String> fromEntityValueRangeProviderRefs = Arrays.stream(valueRangeProviderRefs)
                .filter(descriptorPolicy::hasFromEntityValueRangeProvider)
                .collect(Collectors.toList());
        if (!fromEntityValueRangeProviderRefs.isEmpty()) {
            throw new IllegalArgumentException("@" + ValueRangeProvider.class.getSimpleName()
                    + " on a @" + PlanningEntity.class.getSimpleName()
                    + " is not supported with a list variable (" + this + ").\n"
                    + "Maybe move the valueRangeProvider" + (fromEntityValueRangeProviderRefs.size() > 1 ? "s" : "")
                    + " (" + fromEntityValueRangeProviderRefs
                    + ") from the entity class to the @" + PlanningSolution.class.getSimpleName() + " class.");
        }
        super.processValueRangeRefs(descriptorPolicy, valueRangeProviderRefs);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean acceptsValueType(Class<?> valueType) {
        return getElementType().isAssignableFrom(valueType);
    }

    public Class<?> getElementType() {
        return ConfigUtils.extractCollectionGenericTypeParameterStrictly(
                "entityClass", entityDescriptor.getEntityClass(),
                variableMemberAccessor.getType(), variableMemberAccessor.getGenericType(),
                PlanningListVariable.class, variableMemberAccessor.getName());
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    @Override
    public List<Object> getValue(Object entity) {
        Object value = super.getValue(entity);
        if (value == null) {
            throw new IllegalStateException("The planning list variable (" + this + ") of entity (" + entity + ") is null.");
        }
        return (List<Object>) value;
    }

    public Object removeElement(Object entity, int index) {
        return getValue(entity).remove(index);
    }

    public void addElement(Object entity, int index, Object element) {
        getValue(entity).add(index, element);
    }

    public Object getElement(Object entity, int index) {
        return getValue(entity).get(index);
    }

    public Object setElement(Object entity, int index, Object element) {
        return getValue(entity).set(index, element);
    }

    public int getListSize(Object entity) {
        return getValue(entity).size();
    }

    public ListVariableDataDemand<Solution_> getProvidedDemand() {
        return providedDemand;
    }

}
