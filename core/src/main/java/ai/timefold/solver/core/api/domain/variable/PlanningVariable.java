package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;

/**
 * Specifies that a bean property (or a field) can be changed and should be optimized by the optimization algorithms.
 * <p>
 * The property must be an object type. Primitive types (such as int, double, long) are not allowed.
 * <p>
 * It is specified on a getter of a java bean property (or directly on a field) of a {@link PlanningEntity} class.
 * <p>
 * It is sometimes referred to as the "basic" planning variable,
 * to distinguish it from a {@link PlanningListVariable list variable}.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface PlanningVariable {

    /**
     * Any {@link ValueRangeProvider} annotation on a {@link PlanningSolution} or {@link PlanningEntity}
     * will automatically be registered with its {@link ValueRangeProvider#id()}.
     * <p>
     * If no refs are provided, all {@link ValueRangeProvider}s without an id will be registered,
     * provided their return types match the type of this variable.
     *
     * @return 0 or more registered {@link ValueRangeProvider#id()}
     */
    String[] valueRangeProviderRefs() default {};

    /**
     * A variable will automatically add the planning value null
     * to the {@link ValueRangeProvider}'s range.
     * <p>
     * Allowing unassigned is not compatible with a primitive property type.
     *
     * @see PlanningListVariable#allowsUnassignedValues()
     * @return true if null is a valid value for this planning variable
     */
    boolean allowsUnassigned() default false;

    /**
     * Allows sorting a collection of planning values for this variable.
     * Some algorithms perform better when the values are sorted based on specific metrics.
     * <p>
     * The {@link Comparator} should sort the data in ascending order.
     * For example, prioritize three visits by sorting them based on their importance:
     * Visit C (SMALL_PRIORITY), Visit A (MEDIUM_PRIORITY), Visit B (HIGH_PRIORITY)
     * <p>
     * Do not use together with {@link #comparatorFactoryClass()}.
     *
     * @return {@link NullComparator} when it is null (workaround for annotation limitation)
     * @see #comparatorFactoryClass()
     */
    Class<? extends Comparator> comparatorClass() default NullComparator.class;

    interface NullComparator<T> extends Comparator<T> {
    }

    /**
     * The {@link ComparatorFactory} alternative for {@link #comparatorClass()}.
     * <p>
     * Differs from {@link #comparatorClass()}
     * because it allows accessing the current solution when creating the comparator.
     * <p>
     * Do not use together with {@link #comparatorClass()}.
     * 
     * @return {@link NullComparatorFactory} when it is null (workaround for annotation limitation)
     * @see #comparatorClass()
     */
    Class<? extends ComparatorFactory> comparatorFactoryClass() default NullComparatorFactory.class;

    interface NullComparatorFactory<Solution_, T> extends ComparatorFactory<Solution_, T> {
    }

}
