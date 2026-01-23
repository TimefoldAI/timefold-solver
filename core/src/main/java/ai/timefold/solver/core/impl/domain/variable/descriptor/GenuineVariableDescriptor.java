package ai.timefold.solver.core.impl.domain.variable.descriptor;

import static ai.timefold.solver.core.config.util.ConfigUtils.newInstance;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromSolutionPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorFactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class GenuineVariableDescriptor<Solution_> extends VariableDescriptor<Solution_> {

    private ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private SelectionSorter<Solution_, Object> ascendingSorter;
    private SelectionSorter<Solution_, Object> descendingSorter;

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
        var valueRangeDescriptorList = new ArrayList<ValueRangeDescriptor<Solution_>>(valueRangeProviderMemberAccessors.length);
        for (var valueRangeProviderMemberAccessor : valueRangeProviderMemberAccessors) {
            valueRangeDescriptorList.add(buildValueRangeDescriptor(descriptorPolicy, valueRangeProviderMemberAccessor));
        }
        if (valueRangeDescriptorList.size() == 1) {
            valueRangeDescriptor = valueRangeDescriptorList.get(0);
        } else {
            valueRangeDescriptor = descriptorPolicy.buildCompositeValueRangeDescriptor(this, valueRangeDescriptorList);
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
            MemberAccessor valueRangeProviderMemberAccessor) {
        if (descriptorPolicy.isFromSolutionValueRangeProvider(valueRangeProviderMemberAccessor)) {
            return descriptorPolicy.buildFromSolutionPropertyValueRangeDescriptor(this, valueRangeProviderMemberAccessor);
        } else if (descriptorPolicy.isFromEntityValueRangeProvider(valueRangeProviderMemberAccessor)) {
            return descriptorPolicy.buildFromEntityPropertyValueRangeDescriptor(this, valueRangeProviderMemberAccessor);
        } else {
            throw new IllegalStateException("Impossible state: member accessor (%s) is not a value range provider."
                    .formatted(valueRangeProviderMemberAccessor));
        }
    }

    @SuppressWarnings("rawtypes")
    protected void processSorting(String comparatorPropertyName, Class<? extends Comparator> comparatorClass,
            String comparatorFactoryPropertyName, Class<? extends ComparatorFactory> comparatorFactoryClass) {
        if (comparatorClass != null && PlanningVariable.NullComparator.class.isAssignableFrom(comparatorClass)) {
            comparatorClass = null;
        }
        if (comparatorFactoryClass != null
                && PlanningVariable.NullComparatorFactory.class.isAssignableFrom(comparatorFactoryClass)) {
            comparatorFactoryClass = null;
        }
        if (comparatorClass != null && comparatorFactoryClass != null) {
            throw new IllegalStateException(
                    "The entityClass (%s) property (%s) cannot have a %s (%s) and a %s (%s) at the same time.".formatted(
                            entityDescriptor.getEntityClass(), variableMemberAccessor.getName(), comparatorPropertyName,
                            comparatorClass.getName(), comparatorFactoryPropertyName, comparatorFactoryClass.getName()));
        }
        if (comparatorClass != null) {
            Comparator<Object> comparator = newInstance(this::toString, comparatorPropertyName, comparatorClass);
            ascendingSorter = new ComparatorSelectionSorter<>(comparator,
                    SelectionSorterOrder.ASCENDING);
            descendingSorter = new ComparatorSelectionSorter<>(comparator,
                    SelectionSorterOrder.DESCENDING);
        } else if (comparatorFactoryClass != null) {
            ComparatorFactory<Solution_, Object> comparatorFactory =
                    newInstance(this::toString, comparatorFactoryPropertyName, comparatorFactoryClass);
            ascendingSorter = new ComparatorFactorySelectionSorter<>(comparatorFactory, SelectionSorterOrder.ASCENDING);
            descendingSorter = new ComparatorFactorySelectionSorter<>(comparatorFactory, SelectionSorterOrder.DESCENDING);
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

    /**
     * Returns true if the value range can be directly extracted from the solution.
     *
     * @see FromSolutionPropertyValueRangeDescriptor
     */
    public boolean canExtractValueRangeFromSolution() {
        return valueRangeDescriptor.canExtractValueRangeFromSolution();
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    /**
     * A basic planning variable {@link PlanningVariable#allowsUnassigned() allowing unassigned}
     * and {@link PlanningListVariable} are always considered initialized.
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

    public SelectionSorter<Solution_, Object> getAscendingSorter() {
        return ascendingSorter;
    }

    public SelectionSorter<Solution_, Object> getDescendingSorter() {
        return descendingSorter;
    }

    @Override
    public String toString() {
        return getSimpleEntityAndVariableName() + " variable";
    }

}
