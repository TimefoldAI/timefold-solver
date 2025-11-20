package ai.timefold.solver.core.preview.api.move.ruin;

import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

public interface BasicAssignmentAction<Entity_, Value_, Score_ extends Score<@NonNull Score_>> {
    void assignValues(BasicAssignmentEvaluator<Entity_, Value_, Score_> evaluator);

    default BasicAssignmentAction<Entity_, Value_, Score_> rebase(Rebaser rebaser) {
        throw new UnsupportedOperationException("Rebase is not supported. Implement rebase to support multithreaded solving.");
    }

    static <Entity_, Value_, Score_ extends Score<@NonNull Score_>> BasicAssignmentAction<Entity_, Value_, Score_>
            unassign(Entity_ entity) {
        return new BasicAssignmentAction<>() {
            @Override
            public void assignValues(BasicAssignmentEvaluator<Entity_, Value_, Score_> evaluator) {
                evaluator.unassign(entity);
            }

            @Override
            public BasicAssignmentAction<Entity_, Value_, Score_> rebase(Rebaser rebaser) {
                return BasicAssignmentAction.unassign(rebaser.rebase(entity));
            }
        };
    }

    static <Entity_, Value_, Score_ extends Score<@NonNull Score_>> BasicAssignmentAction<Entity_, Value_, Score_>
            bestFit(Entity_ entity, Iterable<Value_> values) {
        return new BasicAssignmentAction<>() {
            @Override
            public void assignValues(BasicAssignmentEvaluator<Entity_, Value_, Score_> evaluator) {
                var iterator = values.iterator();
                if (!iterator.hasNext()) {
                    throw new IllegalArgumentException(
                            "Cannot perform a best-fit assignment on entity (%s) since the value iterable (%s) is empty."
                                    .formatted(entity, values));
                }
                var bestValue = iterator.next();
                evaluator.assign(entity, bestValue);
                var bestScore = evaluator.score();
                while (iterator.hasNext()) {
                    var candidateValue = iterator.next();
                    evaluator.assign(entity, candidateValue);
                    var score = evaluator.score();
                    if (score.compareTo(bestScore) > 0) {
                        bestScore = score;
                        bestValue = candidateValue;
                    }
                }
                evaluator.assign(entity, bestValue);
            }

            @Override
            public BasicAssignmentAction<Entity_, Value_, Score_> rebase(Rebaser rebaser) {
                return BasicAssignmentAction.bestFit(rebaser.rebase(entity),
                        StreamSupport.stream(values.spliterator(), false).map(rebaser::rebase).toList());
            }
        };
    }
}
