package ai.timefold.solver.core.api.domain.valuerange;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NonNull;

/**
 * Provides the planning values that can be used for a {@link PlanningVariable}.
 * 
 * <p>
 * This is specified on a getter of a java bean property (or directly on a field)
 * which returns a {@link Collection} or {@link ValueRange}.
 * A {@link Collection} is implicitly converted to a {@link ValueRange}.
 * For solver reproducibility, the collection must have a deterministic, stable iteration order.
 * It is recommended to use a {@link List}, {@link LinkedHashSet} or {@link SortedSet}.
 * 
 * <p>
 * Value ranges are not allowed to contain {@code null} values.
 * When {@link PlanningVariable#allowsUnassigned()} or {@link PlanningListVariable#allowsUnassignedValues()} is true,
 * the solver will handle {@code null} values on its own.
 *
 * <p>
 * Value ranges are not allowed to contain multiple copies of the same object,
 * as defined by {@code ==}.
 * It is recommended that the value range never contains two objects
 * that are equal according to {@link Object#equals(Object)},
 * but this is not enforced to not depend on user-defined {@link Object#equals(Object)} implementations.
 * Having duplicates in a value range can lead to unexpected behavior,
 * and skews selection probabilities in random selection algorithms.
 * 
 * <p>
 * Value ranges are not allowed to change during solving.
 * This is especially important for value ranges defined on {@link PlanningEntity}-annotated classes;
 * these must never depend on any of that entity's variables, or on any other entity's variables.
 * If you need to change a value range defined on an entity,
 * trigger a {@link ProblemChange} for that entity or restart the solver with an updated planning solution.
 * If you need to change a value range defined on a planning solution,
 * restart the solver with a new planning solution.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface ValueRangeProvider {

    /**
     * Used by {@link PlanningVariable#valueRangeProviderRefs()}
     * to map a {@link PlanningVariable} to a {@link ValueRangeProvider}.
     * If not provided, an attempt will be made to find a matching {@link PlanningVariable} without a ref.
     *
     * @return if provided, must be unique across a {@link SolverFactory}
     */
    @NonNull
    String id() default "";

}
