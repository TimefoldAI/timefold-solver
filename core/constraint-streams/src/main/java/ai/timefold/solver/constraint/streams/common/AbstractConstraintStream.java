package ai.timefold.solver.constraint.streams.common;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.impl.domain.constraintweight.descriptor.ConstraintConfigurationDescriptor;
import ai.timefold.solver.core.impl.domain.constraintweight.descriptor.ConstraintWeightDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public abstract class AbstractConstraintStream<Solution_> implements ConstraintStream {

    private final RetrievalSemantics retrievalSemantics;

    protected AbstractConstraintStream(RetrievalSemantics retrievalSemantics) {
        this.retrievalSemantics = Objects.requireNonNull(retrievalSemantics);
    }

    public RetrievalSemantics getRetrievalSemantics() {
        return retrievalSemantics;
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    protected Function<Solution_, Score<?>> buildConstraintWeightExtractor(ConstraintRef constraintRef) {
        SolutionDescriptor<Solution_> solutionDescriptor = getConstraintFactory().getSolutionDescriptor();
        ConstraintConfigurationDescriptor<Solution_> configurationDescriptor = solutionDescriptor
                .getConstraintConfigurationDescriptor();
        if (configurationDescriptor == null) {
            throw new IllegalStateException("The constraint (" + constraintRef + ") does not hard-code a constraint weight"
                    + " and there is no @" + ConstraintConfigurationProvider.class.getSimpleName()
                    + " on the solution class (" + solutionDescriptor.getSolutionClass() + ").\n"
                    + "Maybe add a @" + ConstraintConfiguration.class.getSimpleName() + " class"
                    + " or use " + ConstraintStream.class.getSimpleName() + ".penalize()/reward()"
                    + " instead of penalizeConfigurable()/rewardConfigurable.");
        }
        ConstraintWeightDescriptor<Solution_> weightDescriptor = configurationDescriptor
                .findConstraintWeightDescriptor(constraintRef);
        if (weightDescriptor == null) {
            throw new IllegalStateException("The constraint (" + constraintRef + ") does not hard-code a constraint weight"
                    + " and there is no such @" + ConstraintWeight.class.getSimpleName()
                    + " on the constraintConfigurationClass (" + configurationDescriptor.getConstraintConfigurationClass()
                    + ").\n"
                    + "Maybe there is a typo in the constraintPackage or constraintName of one of the @"
                    + ConstraintWeight.class.getSimpleName() + " members.\n"
                    + "Maybe add a @" + ConstraintWeight.class.getSimpleName() + " member for it.");
        }
        return weightDescriptor.createExtractor();
    }

    protected Function<Solution_, Score<?>> buildConstraintWeightExtractor(ConstraintRef constraintRef,
            Score<?> constraintWeight) {
        // Duplicates validation when the session is built, but this fails fast when weights are hard coded
        getConstraintFactory().getSolutionDescriptor().validateConstraintWeight(constraintRef, constraintWeight);
        return solution -> constraintWeight;
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public abstract InnerConstraintFactory<Solution_, ?> getConstraintFactory();

    protected abstract <JustificationMapping_> JustificationMapping_ getDefaultJustificationMapping();

    protected abstract <IndictedObjectsMapping_> IndictedObjectsMapping_ getDefaultIndictedObjectsMapping();

}
