package ai.timefold.solver.core.api.domain.specification;

import java.util.function.BiConsumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;

/**
 * Describes how the score is accessed on a planning solution.
 *
 * @param scoreType the score class
 * @param getter reads the score from the solution
 * @param setter writes the score to the solution
 * @param bendableHardLevelsSize for bendable scores, the number of hard levels (-1 if not bendable)
 * @param bendableSoftLevelsSize for bendable scores, the number of soft levels (-1 if not bendable)
 * @param <S> the solution type
 */
public record ScoreSpecification<S>(
        Class<? extends Score<?>> scoreType,
        Function<S, ?> getter,
        BiConsumer<S, Object> setter,
        int bendableHardLevelsSize,
        int bendableSoftLevelsSize) {

    public ScoreSpecification(Class<? extends Score<?>> scoreType, Function<S, ?> getter, BiConsumer<S, Object> setter) {
        this(scoreType, getter, setter, -1, -1);
    }
}
