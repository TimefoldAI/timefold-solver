package ai.timefold.solver.core.api.domain.specification;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.score.Score;

/**
 * Fluent builder for a {@link PlanningSpecification}.
 *
 * @param <S> the solution type
 */
public interface SolutionSpecificationBuilder<S> {

    <Score_ extends Score<Score_>> SolutionSpecificationBuilder<S> score(
            Class<Score_> scoreType, Function<S, Score_> getter, BiConsumer<S, Score_> setter);

    SolutionSpecificationBuilder<S> problemFact(String name, Function<S, ?> getter);

    SolutionSpecificationBuilder<S> problemFact(String name, Function<S, ?> getter, BiConsumer<S, Object> setter);

    SolutionSpecificationBuilder<S> problemFacts(String name, Function<S, ? extends Collection<?>> getter);

    SolutionSpecificationBuilder<S> problemFacts(String name, Function<S, ? extends Collection<?>> getter,
            BiConsumer<S, Object> setter);

    SolutionSpecificationBuilder<S> entityCollection(String name, Function<S, ? extends Collection<?>> getter);

    SolutionSpecificationBuilder<S> entityCollection(String name, Function<S, ? extends Collection<?>> getter,
            BiConsumer<S, Object> setter);

    SolutionSpecificationBuilder<S> valueRange(String id, Function<S, ?> getter);

    SolutionSpecificationBuilder<S> valueRange(Function<S, ?> getter);

    SolutionSpecificationBuilder<S> constraintWeightOverrides(Function<S, ConstraintWeightOverrides<?>> getter);

    <E> SolutionSpecificationBuilder<S> entity(Class<E> entityClass,
            Consumer<EntitySpecificationBuilder<S, E>> config);

    SolutionSpecificationBuilder<S> cloning(Consumer<CloningSpecificationBuilder<S>> config);

    SolutionSpecificationBuilder<S> solutionCloner(SolutionCloner<S> cloner);

    PlanningSpecification<S> build();
}
