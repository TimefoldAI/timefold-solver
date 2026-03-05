package ai.timefold.solver.core.api.domain.specification;

import java.util.List;

import ai.timefold.solver.core.impl.domain.specification.DefaultSolutionSpecificationBuilder;

/**
 * The complete description of a planning problem's structure.
 * <p>
 * This is an immutable value object produced by the builder API via {@link #of(Class)}.
 * It holds lambdas and configuration that the solver uses to access the domain model
 * without reflection.
 *
 * @param solutionClass the solution class
 * @param score how to access the score
 * @param facts problem fact properties
 * @param entityCollections entity collection properties
 * @param valueRanges value range providers
 * @param entities entity specifications
 * @param cloning how to clone the solution (null if not specified)
 * @param constraintWeights constraint weight overrides (null if not specified)
 * @param <S> the solution type
 */
public record PlanningSpecification<S>(
        Class<S> solutionClass,
        ScoreSpecification<S> score,
        List<FactSpecification<S>> facts,
        List<EntityCollectionSpecification<S>> entityCollections,
        List<ValueRangeSpecification<S>> valueRanges,
        List<EntitySpecification<S>> entities,
        CloningSpecification<S> cloning,
        ConstraintWeightSpecification<S> constraintWeights) {

    /**
     * Entry point for building a {@link PlanningSpecification} programmatically.
     *
     * @param solutionClass the solution class
     * @param <S> the solution type
     * @return a builder for the specification
     */
    public static <S> SolutionSpecificationBuilder<S> of(Class<S> solutionClass) {
        return new DefaultSolutionSpecificationBuilder<>(solutionClass);
    }
}
