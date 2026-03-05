package ai.timefold.solver.core.api.domain.specification;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;

/**
 * Describes constraint weight overrides on a planning solution.
 *
 * @param getter reads the constraint weight overrides from the solution
 * @param <S> the solution type
 */
public record ConstraintWeightSpecification<S>(
        Function<S, ConstraintWeightOverrides<?>> getter) {
}
