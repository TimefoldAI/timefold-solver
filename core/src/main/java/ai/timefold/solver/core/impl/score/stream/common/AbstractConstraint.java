package ai.timefold.solver.core.impl.score.stream.common;

import java.math.BigDecimal;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.definition.AbstractBendableScoreDefinition;

public abstract class AbstractConstraint<Solution_, Constraint_ extends AbstractConstraint<Solution_, Constraint_, ConstraintFactory_>, ConstraintFactory_ extends InnerConstraintFactory<Solution_, Constraint_>>
        implements Constraint {

    private final ConstraintFactory_ constraintFactory;
    private final ConstraintRef constraintRef;
    private final Function<Solution_, Score<?>> constraintWeightExtractor;
    private final ScoreImpactType scoreImpactType;
    private final boolean isConstraintWeightConfigurable;
    // Constraint is not generic in uni/bi/..., therefore these can not be typed.
    private final Object justificationMapping;
    private final Object indictedObjectsMapping;

    protected AbstractConstraint(ConstraintFactory_ constraintFactory, ConstraintRef constraintRef,
            Function<Solution_, Score<?>> constraintWeightExtractor, ScoreImpactType scoreImpactType,
            boolean isConstraintWeightConfigurable, Object justificationMapping, Object indictedObjectsMapping) {
        this.constraintFactory = constraintFactory;
        this.constraintRef = constraintRef;
        this.constraintWeightExtractor = constraintWeightExtractor;
        this.scoreImpactType = scoreImpactType;
        this.isConstraintWeightConfigurable = isConstraintWeightConfigurable;
        this.justificationMapping = justificationMapping;
        this.indictedObjectsMapping = indictedObjectsMapping;
    }

    @SuppressWarnings("unchecked")
    public final <Score_ extends Score<Score_>> Score_ extractConstraintWeight(Solution_ workingSolution) {
        if (isConstraintWeightConfigurable && workingSolution == null) {
            /*
             * In constraint verifier API, we allow for testing constraint providers without having a planning solution.
             * However, constraint weights may be configurable and in that case the solution is required to read the
             * weights from.
             * For these cases, we set the constraint weight to the softest possible value, just to make sure that the
             * constraint is not ignored.
             * The actual value is not used in any way.
             */
            return (Score_) constraintFactory.getSolutionDescriptor().getScoreDefinition().getOneSoftestScore();
        }
        Score_ constraintWeight = (Score_) constraintWeightExtractor.apply(workingSolution);
        validateConstraintWeight(constraintFactory, constraintRef, constraintWeight);
        return switch (scoreImpactType) {
            case PENALTY -> constraintWeight.negate();
            case REWARD, MIXED -> constraintWeight;
        };
    }

    public final void assertCorrectImpact(int impact) {
        if (impact >= 0) {
            return;
        }
        if (scoreImpactType != ScoreImpactType.MIXED) {
            throw new IllegalStateException("Negative match weight (" + impact + ") for constraint ("
                    + constraintRef + "). " +
                    "Check constraint provider implementation.");
        }
    }

    public final void assertCorrectImpact(long impact) {
        if (impact >= 0L) {
            return;
        }
        if (scoreImpactType != ScoreImpactType.MIXED) {
            throw new IllegalStateException("Negative match weight (" + impact + ") for constraint ("
                    + getConstraintRef() + "). " +
                    "Check constraint provider implementation.");
        }
    }

    public final void assertCorrectImpact(BigDecimal impact) {
        if (impact.signum() >= 0) {
            return;
        }
        if (scoreImpactType != ScoreImpactType.MIXED) {
            throw new IllegalStateException("Negative match weight (" + impact + ") for constraint ("
                    + getConstraintRef() + "). " +
                    "Check constraint provider implementation.");
        }
    }

    @Override
    public final ConstraintFactory_ getConstraintFactory() {
        return constraintFactory;
    }

    @Override
    public ConstraintRef getConstraintRef() {
        return constraintRef;
    }

    public final ScoreImpactType getScoreImpactType() {
        return scoreImpactType;
    }

    public <JustificationMapping_> JustificationMapping_ getJustificationMapping() {
        // It is the job of the code constructing the constraint to ensure that this cast is correct.
        return (JustificationMapping_) justificationMapping;
    }

    public <IndictedObjectsMapping_> IndictedObjectsMapping_ getIndictedObjectsMapping() {
        // It is the job of the code constructing the constraint to ensure that this cast is correct.
        return (IndictedObjectsMapping_) indictedObjectsMapping;
    }

    public static <Score_ extends Score<Score_>> void validateConstraintWeight(InnerConstraintFactory<?, ?> constraintFactory,
            ConstraintRef constraintRef, Score_ constraintWeight) {
        if (constraintWeight == null) {
            throw new IllegalArgumentException("""
                    The constraintWeight (null) for constraint (%s) must not be null.
                    Maybe check your constraint implementation."""
                    .formatted(constraintRef));
        }
        var scoreDescriptor = constraintFactory.getSolutionDescriptor().<Score_> getScoreDescriptor();
        if (!constraintWeight.getClass().isAssignableFrom(constraintWeight.getClass())) {
            throw new IllegalArgumentException("""
                    The constraintWeight (%s) of class (%s) for constraint (%s) must be of the scoreClass (%s).
                    Maybe check your constraint implementation."""
                    .formatted(constraintWeight, constraintWeight.getClass(), constraintRef,
                            scoreDescriptor.getScoreDefinition().getScoreClass()));
        }
        if (constraintWeight.initScore() != 0) {
            throw new IllegalArgumentException("""
                    The constraintWeight (%s) for constraint (%s) must have an initScore (%d) equal to 0.
                    Maybe check your constraint implementation."""
                    .formatted(constraintWeight, constraintRef, constraintWeight.initScore()));
        }
        if (!scoreDescriptor.getScoreDefinition().isPositiveOrZero(constraintWeight)) {
            throw new IllegalArgumentException("""
                    The constraintWeight (%s) for constraint (%s) must be positive or zero.
                    Maybe check your constraint implementation."""
                    .formatted(constraintWeight, constraintRef));
        }
        if (constraintWeight instanceof IBendableScore<?> bendableConstraintWeight) {
            var bendableScoreDefinition = (AbstractBendableScoreDefinition<?>) scoreDescriptor.getScoreDefinition();
            if (bendableConstraintWeight.hardLevelsSize() != bendableScoreDefinition.getHardLevelsSize()
                    || bendableConstraintWeight.softLevelsSize() != bendableScoreDefinition.getSoftLevelsSize()) {
                throw new IllegalArgumentException(
                        """
                                The bendable constraintWeight (%s) for constraint (%s) has a hardLevelsSize (%d) or a softLevelsSize (%d) \
                                that doesn't match the score definition's hardLevelsSize (%d) or softLevelsSize (%d).
                                Maybe check your constraint implementation."""
                                .formatted(constraintWeight, constraintRef, bendableConstraintWeight.hardLevelsSize(),
                                        bendableConstraintWeight.softLevelsSize(), bendableScoreDefinition.getHardLevelsSize(),
                                        bendableScoreDefinition.getSoftLevelsSize()));
            }
        }

    }

}
