package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Scorer<Tuple_ extends Tuple> implements TupleLifecycle<Tuple_> {

    private final ScoreImpacter<Tuple_> scoreImpacter;
    private final WeightedScoreImpacter<?, ?> weightedScoreImpacter;
    private final int inputStoreIndex;

    public Scorer(ScoreImpacter<Tuple_> scoreImpacter, WeightedScoreImpacter<?, ?> weightedScoreImpacter, int inputStoreIndex) {
        this.scoreImpacter = Objects.requireNonNull(scoreImpacter);
        this.weightedScoreImpacter = Objects.requireNonNull(weightedScoreImpacter);
        this.inputStoreIndex = inputStoreIndex;
    }

    @Override
    public void insert(Tuple_ tuple) {
        if (tuple.getStore(inputStoreIndex) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(tuple));
        }
        tuple.setStore(inputStoreIndex, impact(tuple));
    }

    @Override
    public void update(Tuple_ tuple) {
        UndoScoreImpacter undoScoreImpacter = tuple.getStore(inputStoreIndex);
        // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
        if (undoScoreImpacter != null) {
            undoScoreImpacter.run();
        }
        tuple.setStore(inputStoreIndex, impact(tuple));
    }

    public UndoScoreImpacter impact(Tuple_ tuple) {
        try {
            return scoreImpacter.apply(weightedScoreImpacter, tuple);
        } catch (Exception e) {
            // Helps with debugging exceptions thrown by user code during impact calls.
            throw new IllegalStateException(
                    "Consequence of a constraint (%s) threw an exception processing a tuple (%s)."
                            .formatted(weightedScoreImpacter.getContext().getConstraint().getConstraintRef(), tuple),
                    e);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        UndoScoreImpacter undoScoreImpacter = tuple.removeStore(inputStoreIndex);
        // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
        if (undoScoreImpacter != null) {
            undoScoreImpacter.run();
        }
    }

    @Override
    public String toString() {
        var context = weightedScoreImpacter.getContext();
        return "%s(%s) with constraintWeight (%s)"
                .formatted(getClass().getSimpleName(), context.getConstraint().getConstraintRef(),
                        context.getConstraintWeight());
    }

}
