package ai.timefold.solver.core.preview.api.move.ruin;

import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

public interface ListAssignmentAction<Entity_, Value_, Score_ extends Score<@NonNull Score_>> {
    void assignValues(ListAssignmentEvaluator<Entity_, Value_, Score_> evaluator);

    default ListAssignmentAction<Entity_, Value_, Score_> rebase(Rebaser rebaser) {
        throw new UnsupportedOperationException("Rebase is not supported. Implement rebase to support multithreaded solving.");
    }

    static <Entity_, Value_, Score_ extends Score<@NonNull Score_>> ListAssignmentAction<Entity_, Value_, Score_>
            unassign(Value_ value) {
        return new ListAssignmentAction<>() {
            @Override
            public void assignValues(ListAssignmentEvaluator<Entity_, Value_, Score_> evaluator) {
                evaluator.unassign(value);
            }

            @Override
            public ListAssignmentAction<Entity_, Value_, Score_> rebase(Rebaser rebaser) {
                return unassign(rebaser.rebase(value));
            }
        };
    }

    static <Entity_, Value_, Score_ extends Score<@NonNull Score_>> ListAssignmentAction<Entity_, Value_, Score_>
            bestFit(Value_ value, Iterable<Entity_> entities) {
        return new ListAssignmentAction<>() {
            @Override
            public void assignValues(ListAssignmentEvaluator<Entity_, Value_, Score_> evaluator) {
                var iterator = entities.iterator();
                if (!iterator.hasNext()) {
                    throw new IllegalArgumentException(
                            "Cannot perform a best-fit assignment on value (%s) since the entities iterable (%s) is empty."
                                    .formatted(value, entities));
                }

                // ensure value is unassigned initially to simplify list position calculations
                // (so we can ignore the case that the value is already present in entity)
                evaluator.unassign(value);

                var bestEntity = iterator.next();
                var bestIndex = 0;

                var listSize = evaluator.listSize(bestEntity);
                evaluator.assign(bestEntity, 0, value);
                var bestScore = evaluator.score();

                for (var i = 1; i <= listSize; i++) {
                    evaluator.assign(bestEntity, i, value);
                    var score = evaluator.score();
                    if (score.compareTo(bestScore) > 0) {
                        bestScore = score;
                        bestIndex = i;
                    }
                }

                while (iterator.hasNext()) {
                    var candidateEntity = iterator.next();
                    listSize = evaluator.listSize(candidateEntity);
                    for (var i = 0; i <= listSize; i++) {
                        evaluator.assign(candidateEntity, i, value);
                        var score = evaluator.score();
                        if (score.compareTo(bestScore) > 0) {
                            bestScore = score;
                            bestEntity = candidateEntity;
                            bestIndex = i;
                        }
                    }
                }
                evaluator.assign(bestEntity, bestIndex, value);
            }

            @Override
            public ListAssignmentAction<Entity_, Value_, Score_> rebase(Rebaser rebaser) {
                return bestFit(rebaser.rebase(value),
                        StreamSupport.stream(entities.spliterator(), false)
                                .map(rebaser::rebase).toList());
            }
        };
    }
}
