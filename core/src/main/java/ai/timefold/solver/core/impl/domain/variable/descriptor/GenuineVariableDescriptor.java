package ai.timefold.solver.core.impl.domain.variable.descriptor;

import static ai.timefold.solver.core.config.util.ConfigUtils.newInstance;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.CompositeValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromSolutionPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.WeightFactorySelectionSorter;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class GenuineVariableDescriptor<Solution_> extends VariableDescriptor<Solution_> {

    private ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private SelectionSorter<Solution_, Object> increasingStrengthSorter;
    private SelectionSorter<Solution_, Object> decreasingStrengthSorter;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    protected GenuineVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        processPropertyAnnotations(descriptorPolicy);
    }

    protected abstract void processPropertyAnnotations(DescriptorPolicy descriptorPolicy);

    protected void processValueRangeRefs(DescriptorPolicy descriptorPolicy, String[] valueRangeProviderRefs) {
        MemberAccessor[] valueRangeProviderMemberAccessors;
        if (valueRangeProviderRefs == null || valueRangeProviderRefs.length == 0) {
            valueRangeProviderMemberAccessors = findAnonymousValueRangeMemberAccessors(descriptorPolicy);
            if (valueRangeProviderMemberAccessors.length == 0) {
                throw new IllegalArgumentException("""
                        The entityClass (%s) has a @%s annotated property (%s) that has no valueRangeProviderRefs (%s) \
                        and no matching anonymous value range providers were found."""
                        .formatted(entityDescriptor.getEntityClass().getSimpleName(),
                                PlanningVariable.class.getSimpleName(),
                                variableMemberAccessor.getName(),
                                Arrays.toString(valueRangeProviderRefs)));
            }
        } else {
            valueRangeProviderMemberAccessors = Arrays.stream(valueRangeProviderRefs)
                    .map(ref -> findValueRangeMemberAccessor(descriptorPolicy, ref))
                    .toArray(MemberAccessor[]::new);
        }
        var valueRangeDescriptorList =
                new ArrayList<ValueRangeDescriptor<Solution_>>(valueRangeProviderMemberAccessors.length);
        var addNullInValueRange =
                this instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                        && basicVariableDescriptor.allowsUnassigned()
                        && valueRangeProviderMemberAccessors.length == 1;
        for (var valueRangeProviderMemberAccessor : valueRangeProviderMemberAccessors) {
            valueRangeDescriptorList
                    .add(buildValueRangeDescriptor(descriptorPolicy, valueRangeProviderMemberAccessor, addNullInValueRange));
        }
        if (valueRangeDescriptorList.size() == 1) {
            valueRangeDescriptor = valueRangeDescriptorList.get(0);
        } else {
            valueRangeDescriptor = new CompositeValueRangeDescriptor<>(this, addNullInValueRange, valueRangeDescriptorList);
        }
    }

    private MemberAccessor[] findAnonymousValueRangeMemberAccessors(DescriptorPolicy descriptorPolicy) {
        var supportsValueRangeProviderFromEntity = !isListVariable();
        var applicableValueRangeProviderAccessors =
                supportsValueRangeProviderFromEntity ? Stream.concat(
                        descriptorPolicy.getAnonymousFromEntityValueRangeProviderSet().stream(),
                        descriptorPolicy.getAnonymousFromSolutionValueRangeProviderSet().stream())
                        : descriptorPolicy.getAnonymousFromSolutionValueRangeProviderSet().stream();
        return applicableValueRangeProviderAccessors
                .filter(valueRangeProviderAccessor -> {
                    /*
                     * For basic variable, the type is the type of the variable.
                     * For list variable, the type is List<X>, and we need to know X.
                     */
                    var variableType =
                            isListVariable() ? (Class<?>) ((ParameterizedType) variableMemberAccessor.getGenericType())
                                    .getActualTypeArguments()[0] : variableMemberAccessor.getType();
                    // We expect either ValueRange, Collection or an array.
                    var valueRangeType = valueRangeProviderAccessor.getGenericType();
                    if (valueRangeType instanceof ParameterizedType parameterizedValueRangeType) {
                        return ConfigUtils
                                .extractGenericTypeParameter("solutionClass",
                                        entityDescriptor.getSolutionDescriptor().getSolutionClass(),
                                        valueRangeProviderAccessor.getType(), parameterizedValueRangeType,
                                        ValueRangeProvider.class, valueRangeProviderAccessor.getName())
                                .map(variableType::isAssignableFrom)
                                .orElse(false);
                    } else {
                        var clz = (Class<?>) valueRangeType;
                        if (clz.isArray()) {
                            var componentType = clz.getComponentType();
                            return variableType.isAssignableFrom(componentType);
                        }
                        return false;
                    }
                })
                .toArray(MemberAccessor[]::new);
    }

    private MemberAccessor findValueRangeMemberAccessor(DescriptorPolicy descriptorPolicy, String valueRangeProviderRef) {
        if (descriptorPolicy.hasFromSolutionValueRangeProvider(valueRangeProviderRef)) {
            return descriptorPolicy.getFromSolutionValueRangeProvider(valueRangeProviderRef);
        } else if (descriptorPolicy.hasFromEntityValueRangeProvider(valueRangeProviderRef)) {
            return descriptorPolicy.getFromEntityValueRangeProvider(valueRangeProviderRef);
        } else {
            var providerIds = descriptorPolicy.getValueRangeProviderIds();
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has a @" + PlanningVariable.class.getSimpleName()
                    + " annotated property (" + variableMemberAccessor.getName()
                    + ") with a valueRangeProviderRef (" + valueRangeProviderRef
                    + ") that does not exist in a @" + ValueRangeProvider.class.getSimpleName()
                    + " on the solution class ("
                    + entityDescriptor.getSolutionDescriptor().getSolutionClass().getSimpleName()
                    + ") or on that entityClass.\n"
                    + "The valueRangeProviderRef (" + valueRangeProviderRef
                    + ") does not appear in the valueRangeProvideIds (" + providerIds
                    + ")." + (!providerIds.isEmpty() ? ""
                            : "\nMaybe a @" + ValueRangeProvider.class.getSimpleName()
                                    + " annotation is missing on a method in the solution class ("
                                    + entityDescriptor.getSolutionDescriptor().getSolutionClass().getSimpleName() + ")."));
        }
    }

    private ValueRangeDescriptor<Solution_> buildValueRangeDescriptor(DescriptorPolicy descriptorPolicy,
            MemberAccessor valueRangeProviderMemberAccessor, boolean addNullInValueRange) {
        if (descriptorPolicy.isFromSolutionValueRangeProvider(valueRangeProviderMemberAccessor)) {
            return new FromSolutionPropertyValueRangeDescriptor<>(this, addNullInValueRange, valueRangeProviderMemberAccessor);
        } else if (descriptorPolicy.isFromEntityValueRangeProvider(valueRangeProviderMemberAccessor)) {
            return new FromEntityPropertyValueRangeDescriptor<>(this, addNullInValueRange, valueRangeProviderMemberAccessor);
        } else {
            throw new IllegalStateException("Impossible state: member accessor (" + valueRangeProviderMemberAccessor
                    + ") is not a value range provider.");
        }
    }

    protected void processStrength(Class<? extends Comparator> strengthComparatorClass,
            Class<? extends SelectionSorterWeightFactory> strengthWeightFactoryClass) {
        if (strengthComparatorClass == PlanningVariable.NullStrengthComparator.class) {
            strengthComparatorClass = null;
        }
        if (strengthWeightFactoryClass == PlanningVariable.NullStrengthWeightFactory.class) {
            strengthWeightFactoryClass = null;
        }
        if (strengthComparatorClass != null && strengthWeightFactoryClass != null) {
            throw new IllegalStateException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") property (" + variableMemberAccessor.getName()
                    + ") cannot have a strengthComparatorClass (" + strengthComparatorClass.getName()
                    + ") and a strengthWeightFactoryClass (" + strengthWeightFactoryClass.getName()
                    + ") at the same time.");
        }
        if (strengthComparatorClass != null) {
            Comparator<Object> strengthComparator = newInstance(this::toString,
                    "strengthComparatorClass", strengthComparatorClass);
            increasingStrengthSorter = new ComparatorSelectionSorter<>(strengthComparator,
                    SelectionSorterOrder.ASCENDING);
            decreasingStrengthSorter = new ComparatorSelectionSorter<>(strengthComparator,
                    SelectionSorterOrder.DESCENDING);
        }
        if (strengthWeightFactoryClass != null) {
            SelectionSorterWeightFactory<Solution_, Object> strengthWeightFactory = newInstance(this::toString,
                    "strengthWeightFactoryClass", strengthWeightFactoryClass);
            increasingStrengthSorter = new WeightFactorySelectionSorter<>(strengthWeightFactory,
                    SelectionSorterOrder.ASCENDING);
            decreasingStrengthSorter = new WeightFactorySelectionSorter<>(strengthWeightFactory,
                    SelectionSorterOrder.DESCENDING);
        }
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        // Overriding this method so that subclasses can override it too and call super.
        // This way, if this method ever gets any content, it will be called by all subclasses, preventing bugs.
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public abstract boolean acceptsValueType(Class<?> valueType);

    public ValueRangeDescriptor<Solution_> getValueRangeDescriptor() {
        return valueRangeDescriptor;
    }

    public boolean isValueRangeEntityIndependent() {
        return valueRangeDescriptor.isEntityIndependent();
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    /**
     * A basic planning variable {@link PlanningVariable#allowsUnassigned() allowing unassigned}
     * and @{@link PlanningListVariable} are always considered initialized.
     *
     * @param entity never null
     * @return true if the variable on that entity is initialized
     */
    public abstract boolean isInitialized(Object entity);

    /**
     * Decides whether an entity is eligible for initialization.
     * This is not an opposite of {@code isInitialized()} because
     * even a {@link PlanningVariable#allowsUnassigned() variable that allows unassigned},
     * which is always considered initialized,
     * is reinitializable if its value is {@code null}.
     */
    public boolean isReinitializable(Object entity) {
        var value = getValue(entity);
        return value == null;
    }

    public SelectionSorter<Solution_, Object> getIncreasingStrengthSorter() {
        return increasingStrengthSorter;
    }

    public SelectionSorter<Solution_, Object> getDecreasingStrengthSorter() {
        return decreasingStrengthSorter;
    }

    public long getValueRangeSize(Solution_ solution, Object entity) {
        return valueRangeDescriptor.extractValueRangeSize(solution, entity);
    }

    @Override
    public String toString() {
        return getSimpleEntityAndVariableName() + " variable";
    }

}
