package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

/**
 * Specifies that a bean property (or a field) can be changed and should be optimized by the optimization algorithms.
 * <p>
 * The property must be an object type. Primitive types (such as int, double, long) are not allowed.
 * <p>
 * It is specified on a getter of a java bean property (or directly on a field) of a {@link PlanningEntity} class.
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
     * Allowing unassigned is not compatible with {@link PlanningVariableGraphType#CHAINED} true.
     * Allowing unassigned is not compatible with a primitive property type.
     *
     * @see PlanningListVariable#allowsUnassignedValues()
     * @return true if null is a valid value for this planning variable
     */
    boolean allowsUnassigned() default false;

    /**
     * As defined by {@link #allowsUnassigned()}.
     *
     * @deprecated Use {@link #allowsUnassigned()} instead.
     * @return true if null is a valid value for this planning variable
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    boolean nullable() default false;

    /**
     * In some use cases, such as Vehicle Routing, planning entities form a specific graph type,
     * as specified by {@link PlanningVariableGraphType}.
     *
     * @return never null, defaults to {@link PlanningVariableGraphType#NONE}
     */
    PlanningVariableGraphType graphType() default PlanningVariableGraphType.NONE;

    /**
     * Allows a collection of planning values for this variable to be sorted by strength.
     * A strengthWeight estimates how strong a planning value is.
     * Some algorithms benefit from planning on weaker planning values first or from focusing on them.
     * <p>
     * The {@link Comparator} should sort in ascending strength.
     * For example: sorting 3 computers on strength based on their RAM capacity:
     * Computer B (1GB RAM), Computer A (2GB RAM), Computer C (7GB RAM),
     * <p>
     * Do not use together with {@link #strengthWeightFactoryClass()}.
     *
     * @return {@link NullStrengthComparator} when it is null (workaround for annotation limitation)
     * @see #strengthWeightFactoryClass()
     */
    Class<? extends Comparator> strengthComparatorClass() default NullStrengthComparator.class;

    /** Workaround for annotation limitation in {@link #strengthComparatorClass()}. */
    interface NullStrengthComparator extends Comparator {
    }

    /**
     * The {@link SelectionSorterWeightFactory} alternative for {@link #strengthComparatorClass()}.
     * <p>
     * Do not use together with {@link #strengthComparatorClass()}.
     *
     * @return {@link NullStrengthWeightFactory} when it is null (workaround for annotation limitation)
     * @see #strengthComparatorClass()
     */
    Class<? extends SelectionSorterWeightFactory> strengthWeightFactoryClass() default NullStrengthWeightFactory.class;

    /** Workaround for annotation limitation in {@link #strengthWeightFactoryClass()}. */
    interface NullStrengthWeightFactory extends SelectionSorterWeightFactory {
    }

}
