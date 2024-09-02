package ai.timefold.solver.core.impl.domain.variable.descriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.util.MutableLong;

public final class ListVariableDescriptor<Solution_> extends GenuineVariableDescriptor<Solution_> {

    private final ListVariableStateDemand<Solution_> stateDemand = new ListVariableStateDemand<>(this);
    private final BiPredicate inListPredicate = (element, entity) -> {
        var list = getValue(entity);
        return list.contains(element);
    };
    private boolean allowsUnassignedValues = true;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ListVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    public ListVariableStateDemand<Solution_> getStateDemand() {
        return stateDemand;
    }

    public <A> BiPredicate<A, Object> getInListPredicate() {
        return inListPredicate;
    }

    public boolean allowsUnassignedValues() {
        return allowsUnassignedValues;
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
        var fromEntityValueRangeProviderRefs = Arrays.stream(valueRangeProviderRefs)
                .filter(descriptorPolicy::hasFromEntityValueRangeProvider)
                .toList();
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

    @Override
    public boolean isInitialized(Object entity) {
        return true; // List variable itself can never be null and is always initialized.
    }

    public Class<?> getElementType() {
        return ConfigUtils.extractGenericTypeParameterOrFail("entityClass", entityDescriptor.getEntityClass(),
                variableMemberAccessor.getType(), variableMemberAccessor.getGenericType(), PlanningListVariable.class,
                variableMemberAccessor.getName());
    }

    public int countUnassigned(Solution_ solution) {
        var valueCount = new MutableLong(getValueRangeSize(solution, null));
        var solutionDescriptor = entityDescriptor.getSolutionDescriptor();
        solutionDescriptor.visitEntitiesByEntityClass(solution,
                entityDescriptor.getEntityClass(), entity -> {
                    var assignedValues = getValue(entity);
                    valueCount.subtract(assignedValues.size());
                    return false;
                });
        return valueCount.intValue();
    }

    public InverseRelationShadowVariableDescriptor<Solution_> getInverseRelationShadowVariableDescriptor() {
        var inverseRelationEntityDescriptor =
                getEntityDescriptor().getSolutionDescriptor().findEntityDescriptor(getElementType());
        if (inverseRelationEntityDescriptor == null) {
            return null;
        }
        var applicableShadowDescriptors = inverseRelationEntityDescriptor.getShadowVariableDescriptors()
                .stream()
                .filter(f -> f instanceof InverseRelationShadowVariableDescriptor<Solution_> inverseRelationShadowVariableDescriptor
                        && Objects.equals(inverseRelationShadowVariableDescriptor.getSourceVariableDescriptorList().get(0),
                                this))
                .toList();
        if (applicableShadowDescriptors.isEmpty()) {
            return null;
        } else if (applicableShadowDescriptors.size() > 1) {
            // This state may be impossible.
            throw new IllegalStateException(
                    """
                            Instances of entityClass (%s) may be used in list variable (%s), but the class has more than one @%s-annotated field (%s).
                            Remove the annotations from all but one field."""
                            .formatted(inverseRelationEntityDescriptor.getEntityClass().getCanonicalName(),
                                    getSimpleEntityAndVariableName(),
                                    InverseRelationShadowVariable.class.getSimpleName(),
                                    applicableShadowDescriptors.stream()
                                            .map(ShadowVariableDescriptor::getSimpleEntityAndVariableName)
                                            .collect(Collectors.joining(", ", "[", "]"))));
        } else {
            return (InverseRelationShadowVariableDescriptor<Solution_>) applicableShadowDescriptors.get(0);
        }
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    @Override
    public List<Object> getValue(Object entity) {
        Object value = super.getValue(entity);
        if (value == null) {
            throw new IllegalStateException("The planning list variable (%s) of entity (%s) is null."
                    .formatted(this, entity));
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
        var values = getValue(entity);
        if (index >= values.size()) {
            throw new IndexOutOfBoundsException(
                    "Impossible state: The index (%s) must be less than the size (%s) of the planning list variable (%s) of entity (%s)."
                            .formatted(index, values.size(), this, entity));
        }
        return values.get(index);
    }

    public Object setElement(Object entity, int index, Object element) {
        return getValue(entity).set(index, element);
    }

    public int getListSize(Object entity) {
        return getValue(entity).size();
    }

    // ************************************************************************
    // Pinning support
    // ************************************************************************

    public boolean supportsPinning() {
        return entityDescriptor.supportsPinning();
    }

    public boolean isElementPinned(Solution_ workingSolution, Object entity, int index) {
        if (!supportsPinning()) {
            return false;
        } else if (!entityDescriptor.isMovable(workingSolution, entity)) { // Skipping due to @PlanningPin.
            return true;
        } else {
            return index < getFirstUnpinnedIndex(entity);
        }
    }

    public Object getRandomUnpinnedElement(Object entity, Random workingRandom) {
        var listVariable = getValue(entity);
        var firstUnpinnedIndex = getFirstUnpinnedIndex(entity);
        return listVariable.get(workingRandom.nextInt(listVariable.size() - firstUnpinnedIndex) + firstUnpinnedIndex);
    }

    public int getUnpinnedSubListSize(Object entity) {
        var listSize = getListSize(entity);
        var firstUnpinnedIndex = getFirstUnpinnedIndex(entity);
        return listSize - firstUnpinnedIndex;
    }

    public List<Object> getUnpinnedSubList(Object entity) {
        var firstUnpinnedIndex = getFirstUnpinnedIndex(entity);
        var entityList = getValue(entity);
        if (firstUnpinnedIndex == 0) {
            return entityList;
        }
        return entityList.subList(firstUnpinnedIndex, entityList.size());
    }

    public int getFirstUnpinnedIndex(Object entity) {
        var effectivePlanningPinToIndexReader = entityDescriptor.getEffectivePlanningPinToIndexReader();
        if (effectivePlanningPinToIndexReader == null) { // There is no @PlanningPinToIndex.
            return 0;
        } else {
            return effectivePlanningPinToIndexReader.applyAsInt(null, entity);
        }
    }

}
