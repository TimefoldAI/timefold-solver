package ai.timefold.solver.core.impl.score.stream.common;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;

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

    @SuppressWarnings("unchecked")
    protected <Score_ extends Score<Score_>> Function<Solution_, Score_>
            buildConstraintWeightExtractor(ConstraintRef constraintRef) {
        var solutionDescriptor = getConstraintFactory().getSolutionDescriptor();
        var weightSupplier = solutionDescriptor.<Score_> getConstraintWeightSupplier();
        if (weightSupplier == null) {
            throw new IllegalStateException("The constraint (" + constraintRef + ") does not hard-code a constraint weight"
                    + " and there is no @" + ConstraintConfigurationProvider.class.getSimpleName()
                    + " on the solution class (" + solutionDescriptor.getSolutionClass() + ").\n"
                    + "Maybe add a @" + ConstraintConfiguration.class.getSimpleName() + " class"
                    + " or use " + ConstraintStream.class.getSimpleName() + ".penalize()/reward()"
                    + " instead of penalizeConfigurable()/rewardConfigurable.");
        }
        if (!weightSupplier.getSupportedConstraints().contains(constraintRef)) {
            throw new IllegalStateException("The constraint (" + constraintRef + ") does not hard-code a constraint weight"
                    + " and there is no such @" + ConstraintWeight.class.getSimpleName()
                    + " on the constraintConfigurationClass (" + weightSupplier.getProblemFactClass()
                    + ").\n"
                    + "Maybe there is a typo in the constraintPackage or constraintName of one of the @"
                    + ConstraintWeight.class.getSimpleName() + " members.\n"
                    + "Maybe add a @" + ConstraintWeight.class.getSimpleName() + " member for it.");
        }
        return solution -> {
            var weight = (Score_) weightSupplier.getConstraintWeight(constraintRef, solution);
            weightSupplier.validateConstraintWeight(constraintRef, weight);
            return weight;
        };
    }

    protected <Score_ extends Score<Score_>> Function<Solution_, Score_>
            buildConstraintWeightExtractor(ConstraintRef constraintRef, Score_ constraintWeight) {
        AbstractConstraint.validateWeight(getConstraintFactory().getSolutionDescriptor(), constraintRef, constraintWeight);
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
