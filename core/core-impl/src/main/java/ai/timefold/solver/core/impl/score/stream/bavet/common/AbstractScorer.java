package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

public abstract class AbstractScorer<Tuple_ extends AbstractTuple> implements TupleLifecycle<Tuple_> {

    protected final WeightedScoreImpacter<?, ?> weightedScoreImpacter;
    private final int inputStoreIndex;

    protected AbstractScorer(WeightedScoreImpacter<?, ?> weightedScoreImpacter, int inputStoreIndex) {
        this.weightedScoreImpacter = weightedScoreImpacter;
        this.inputStoreIndex = inputStoreIndex;
    }

    @Override
    public final void insert(Tuple_ tuple) {
        if (tuple.getStore(inputStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tuple
                    + ") was already added in the tupleStore.");
        }
        tuple.setStore(inputStoreIndex, impact(tuple));
    }

    @Override
    public final void update(Tuple_ tuple) {
        UndoScoreImpacter undoScoreImpacter = tuple.getStore(inputStoreIndex);
        // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
        if (undoScoreImpacter != null) {
            undoScoreImpacter.run();
        }
        tuple.setStore(inputStoreIndex, impact(tuple));
    }

    protected abstract UndoScoreImpacter impact(Tuple_ tuple);

    /**
     * Helps with debugging exceptions thrown by user code during impact calls.
     *
     * @param tuple never null
     * @param cause never null
     * @return never null, exception to be thrown.
     */
    protected RuntimeException createExceptionOnImpact(Tuple_ tuple, Exception cause) {
        return new IllegalStateException(
                "Consequence of a constraint (" + weightedScoreImpacter.getContext().getConstraint().getConstraintRef()
                        + ") threw an exception processing a tuple (" + tuple + ").",
                cause);
    }

    @Override
    public final void retract(Tuple_ tuple) {
        UndoScoreImpacter undoScoreImpacter = tuple.getStore(inputStoreIndex);
        // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
        if (undoScoreImpacter != null) {
            undoScoreImpacter.run();
            tuple.setStore(inputStoreIndex, null);
        }
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "(" + weightedScoreImpacter.getContext().getConstraint().getConstraintRef()
                + ") with constraintWeight (" + weightedScoreImpacter.getContext().getConstraintWeight() + ")";
    }

}
