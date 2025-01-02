package ai.timefold.solver.core.impl.score.stream.common;

import java.util.Objects;

import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetStream;

import org.jspecify.annotations.NonNull;

public abstract class AbstractConstraintStream<Solution_> implements ConstraintStream, BavetStream {

    private final RetrievalSemantics retrievalSemantics;

    protected AbstractConstraintStream(RetrievalSemantics retrievalSemantics) {
        this.retrievalSemantics = Objects.requireNonNull(retrievalSemantics);
    }

    public RetrievalSemantics getRetrievalSemantics() {
        return retrievalSemantics;
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public abstract @NonNull InnerConstraintFactory<Solution_, ?> getConstraintFactory();

    protected abstract <JustificationMapping_> JustificationMapping_ getDefaultJustificationMapping();

    protected abstract <IndictedObjectsMapping_> IndictedObjectsMapping_ getDefaultIndictedObjectsMapping();

}
