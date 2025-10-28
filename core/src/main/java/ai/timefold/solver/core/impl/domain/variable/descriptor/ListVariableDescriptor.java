package ai.timefold.solver.core.impl.domain.variable.descriptor;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingFilter;

public final class ListVariableDescriptor<Solution_> extends GenuineVariableDescriptor<Solution_> {

    private final ListVariableStateDemand<Solution_> stateDemand = new ListVariableStateDemand<>(this);
    private final BiPredicate<Object, Object> inListPredicate = (element, entity) -> {
        var list = getValue(entity);
        return list.contains(element);
    };
    private final BiEnumeratingFilter<Solution_, Object, Object> entityContainsPinnedValuePredicate =
            (solutionView, value, entity) -> {
                var moveDirector = (MoveDirector<Solution_, ?>) solutionView;
                return moveDirector.isPinned(this, value);
            };

    private boolean allowsUnassignedValues = true;

    public ListVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    public ListVariableStateDemand<Solution_> getStateDemand() {
        return stateDemand;
    }

    @SuppressWarnings("unchecked")
    public <A> BiPredicate<A, Object> getInListPredicate() {
        return (BiPredicate<A, Object>) inListPredicate;
    }

    @SuppressWarnings("unchecked")
    public <A, B> BiEnumeratingFilter<Solution_, A, B> getEntityContainsPinnedValuePredicate() {
        return (BiEnumeratingFilter<Solution_, A, B>) entityContainsPinnedValuePredicate;
    }

    public boolean allowsUnassignedValues() {
        return allowsUnassignedValues;
    }

    @Override
    protected void processPropertyAnnotations(DescriptorPolicy descriptorPolicy) {
        PlanningListVariable planningVariableAnnotation = variableMemberAccessor.getAnnotation(PlanningListVariable.class);
        allowsUnassignedValues = planningVariableAnnotation.allowsUnassignedValues();
        processValueRangeRefs(descriptorPolicy, planningVariableAnnotation.valueRangeProviderRefs());
        processSorting("comparatorClass", planningVariableAnnotation.comparatorClass(), "comparatorFactoryClass",
                planningVariableAnnotation.comparatorFactoryClass());
    }

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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public <Value_> Value_ getElement(Object entity, int index) {
        var values = getValue(entity);
        if (index >= values.size()) {
            throw new IndexOutOfBoundsException(
                    "Impossible state: The index (%s) must be less than the size (%s) of the planning list variable (%s) of entity (%s)."
                            .formatted(index, values.size(), this, entity));
        }
        return (Value_) values.get(index);
    }

    @SuppressWarnings("unchecked")
    public <Value_> Value_ setElement(Object entity, int index, Value_ element) {
        return (Value_) getValue(entity).set(index, element);
    }

    public int getListSize(Object entity) {
        return getValue(entity).size();
    }

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
            return effectivePlanningPinToIndexReader.applyAsInt(entity);
        }
    }

}
