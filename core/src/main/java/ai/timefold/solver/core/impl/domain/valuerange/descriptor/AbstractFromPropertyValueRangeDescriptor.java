package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.collection.IdentityListValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.collection.ListValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.empty.EmptyValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractFromPropertyValueRangeDescriptor<Solution_>
        extends AbstractValueRangeDescriptor<Solution_> {

    protected final MemberAccessor memberAccessor;
    protected boolean collectionWrapping;
    protected boolean arrayWrapping;
    protected boolean countable;
    // Field related to the generic type of the value range, e.g., List<String> -> String
    private final boolean isGenericTypeImmutable;

    protected AbstractFromPropertyValueRangeDescriptor(GenuineVariableDescriptor<Solution_> variableDescriptor,
            boolean acceptNullInValueRange,
            MemberAccessor memberAccessor) {
        super(variableDescriptor, acceptNullInValueRange);
        this.memberAccessor = memberAccessor;
        ValueRangeProvider valueRangeProviderAnnotation = memberAccessor.getAnnotation(ValueRangeProvider.class);
        if (valueRangeProviderAnnotation == null) {
            throw new IllegalStateException("The member (%s) must have a valueRangeProviderAnnotation (%s)."
                    .formatted(memberAccessor, valueRangeProviderAnnotation));
        }
        var type = memberAccessor.getType();
        collectionWrapping = Collection.class.isAssignableFrom(type);
        arrayWrapping = type.isArray();
        processValueRangeProviderAnnotation(valueRangeProviderAnnotation);
        if (acceptNullInValueRange && !countable) {
            throw new IllegalStateException("""
                    The valueRangeDescriptor (%s) allows unassigned values, but not countable (%s).
                    Maybe the member (%s) should return %s."""
                    .formatted(this, countable, memberAccessor, CountableValueRange.class.getSimpleName()));
        }
        if (collectionWrapping) {
            var genericType = ConfigUtils.extractGenericTypeParameterOrFail("solutionClass or entityClass",
                    memberAccessor.getDeclaringClass(), memberAccessor.getType(), memberAccessor.getGenericType(),
                    ValueRangeProvider.class, memberAccessor.getName());
            this.isGenericTypeImmutable = ConfigUtils.isGenericTypeImmutable(genericType);
        } else {
            this.isGenericTypeImmutable = true;
        }
    }

    private void processValueRangeProviderAnnotation(ValueRangeProvider valueRangeProviderAnnotation) {
        EntityDescriptor<Solution_> entityDescriptor = variableDescriptor.getEntityDescriptor();
        Class<?> type = memberAccessor.getType();
        if (!collectionWrapping && !arrayWrapping && !ValueRange.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("""
                    The entityClass (%s) has a @%s-annotated property (%s) that refers to a @%s-annotated member \
                    (%s) that does not return a %s, an array or a %s."""
                    .formatted(entityDescriptor.getEntityClass(), PlanningVariable.class.getSimpleName(),
                            variableDescriptor.getVariableName(), ValueRangeProvider.class.getSimpleName(),
                            memberAccessor, Collection.class.getSimpleName(), ValueRange.class.getSimpleName()));
        }
        if (collectionWrapping) {
            Class<?> collectionElementClass = ConfigUtils.extractGenericTypeParameterOrFail("solutionClass or entityClass",
                    memberAccessor.getDeclaringClass(), memberAccessor.getType(), memberAccessor.getGenericType(),
                    ValueRangeProvider.class, memberAccessor.getName());
            if (!variableDescriptor.acceptsValueType(collectionElementClass)) {
                throw new IllegalArgumentException("""
                        The entityClass (%s) has a @%s-annotated property (%s) that refers to a @%s-annotated member (%s) \
                        that returns a %s with elements of type (%s) which cannot be assigned to the type of %s (%s)."""
                        .formatted(entityDescriptor.getEntityClass(), PlanningVariable.class.getSimpleName(),
                                variableDescriptor.getVariableName(), ValueRangeProvider.class.getSimpleName(),
                                memberAccessor, Collection.class.getSimpleName(), collectionElementClass,
                                PlanningVariable.class.getSimpleName(), variableDescriptor.getVariablePropertyType()));
            }
        } else if (arrayWrapping) {
            Class<?> arrayElementClass = type.getComponentType();
            if (!variableDescriptor.acceptsValueType(arrayElementClass)) {
                throw new IllegalArgumentException(
                        """
                                The entityClass (%s) has a @%s-annotated property (%s) that refers to a @%s-annotated member (%s) \
                                that returns an array with elements of type (%s) which cannot be assigned to the type of the @%s (%s);"""
                                .formatted(entityDescriptor.getEntityClass(), PlanningVariable.class.getSimpleName(),
                                        variableDescriptor.getVariableName(), ValueRangeProvider.class.getSimpleName(),
                                        memberAccessor, arrayElementClass, PlanningVariable.class.getSimpleName(),
                                        variableDescriptor.getVariablePropertyType()));
            }
        }
        countable = collectionWrapping || arrayWrapping || CountableValueRange.class.isAssignableFrom(type);
    }

    @Override
    public boolean isCountable() {
        return countable;
    }

    @SuppressWarnings("unchecked")
    protected <Value_> ValueRange<Value_> readValueRange(Object bean) {
        Object valueRangeObject = memberAccessor.executeGetter(bean);
        if (valueRangeObject == null) {
            throw new IllegalStateException(
                    "The @%s-annotated member (%s) called on bean (%s) must not return a null valueRangeObject (%s)."
                            .formatted(ValueRangeProvider.class.getSimpleName(), memberAccessor, bean, valueRangeObject));
        }
        ValueRange<Value_> valueRange;
        if (collectionWrapping || arrayWrapping) {
            List<Value_> list = collectionWrapping ? transformCollectionToList((Collection<Value_>) valueRangeObject)
                    : ReflectionHelper.transformArrayToList(valueRangeObject);
            // Don't check the entire list for performance reasons, but do check common pitfalls
            if (!list.isEmpty() && (list.get(0) == null || list.get(list.size() - 1) == null)) {
                throw new IllegalStateException(
                        """
                                The @%s-annotated member (%s) called on bean (%s) must not return a %s (%s) with an element that is null.
                                Maybe remove that null element from the dataset.
                                Maybe use @%s(allowsUnassigned = true) instead."""
                                .formatted(ValueRangeProvider.class.getSimpleName(),
                                        memberAccessor, bean,
                                        collectionWrapping ? Collection.class.getSimpleName() : "array",
                                        list,
                                        PlanningVariable.class.getSimpleName()));
            }
            valueRange = new ListValueRange<>(list);
            if (!isGenericTypeImmutable) {
                valueRange = new IdentityListValueRange<>((ListValueRange<Value_>) valueRange);
            }
        } else {
            valueRange = (ValueRange<Value_>) valueRangeObject;
        }
        return valueRange.isEmpty() ? (ValueRange<Value_>) EmptyValueRange.INSTANCE : valueRange;
    }

    private <T> List<T> transformCollectionToList(Collection<T> collection) {
        if (collection instanceof List<T> list) {
            if (collection instanceof LinkedList<T> linkedList) {
                // ValueRange.createRandomIterator(Random) and ValueRange.get(int) wouldn't be efficient.
                return new ArrayList<>(linkedList);
            } else {
                return list;
            }
        } else {
            // TODO If only ValueRange.createOriginalIterator() is used, cloning a Set to a List is a waste of time.
            return new ArrayList<>(collection);
        }
    }

}
