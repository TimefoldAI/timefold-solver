package ai.timefold.solver.core.impl.score.stream.common;

import java.math.BigDecimal;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.AbstractBendableScoreDefinition;

public abstract class AbstractConstraint<Solution_, Constraint_ extends AbstractConstraint<Solution_, Constraint_, ConstraintFactory_>, ConstraintFactory_ extends InnerConstraintFactory<Solution_, Constraint_>>
        implements Constraint {

    private final ConstraintFactory_ constraintFactory;
    private final ConstraintRef constraintRef;
    private final String description;
    private final Score<?> defaultConstraintWeight;
    private final ScoreImpactType scoreImpactType;
    // Constraint is not generic in uni/bi/..., therefore these can not be typed.
    private final Object justificationMapping;
    private final Object indictedObjectsMapping;

    /**
     *
     * @param constraintFactory never null
     * @param constraintRef never null
     * @param defaultConstraintWeight if null, it means legacy constraint configuration code;
     *        will require {@link ConstraintConfiguration} to be present.
     * @param scoreImpactType never null
     * @param justificationMapping never null
     * @param indictedObjectsMapping never null
     */
    protected AbstractConstraint(ConstraintFactory_ constraintFactory, ConstraintRef constraintRef, String description,
            Score<?> defaultConstraintWeight, ScoreImpactType scoreImpactType, Object justificationMapping,
            Object indictedObjectsMapping) {
        this.constraintFactory = Objects.requireNonNull(constraintFactory);
        this.constraintRef = Objects.requireNonNull(constraintRef);
        this.description = Objects.requireNonNull(description);
        this.defaultConstraintWeight = defaultConstraintWeight;
        this.scoreImpactType = Objects.requireNonNull(scoreImpactType);
        this.justificationMapping = justificationMapping; // May be omitted in test code.
        this.indictedObjectsMapping = indictedObjectsMapping; // May be omitted in test code.
    }

    @SuppressWarnings("unchecked")
    public final <Score_ extends Score<Score_>> Score_ extractConstraintWeight(Solution_ solution) {
        return adjustConstraintWeight((Score_) determineConstraintWeight(solution));
    }

    private <Score_ extends Score<Score_>> Score_ adjustConstraintWeight(Score_ constraintWeight) {
        return switch (scoreImpactType) {
            case PENALTY -> constraintWeight.negate();
            case REWARD, MIXED -> constraintWeight;
        };
    }

    @SuppressWarnings("unchecked")
    private <Score_ extends Score<Score_>> Score_ determineConstraintWeight(Solution_ solution) {
        var solutionDescriptor = constraintFactory.getSolutionDescriptor();
        var hasConstraintWeight = defaultConstraintWeight != null;
        var constraintWeightSupplier = solutionDescriptor.<Score_> getConstraintWeightSupplier();
        var hasConstraintWeightSupplier = constraintWeightSupplier != null;

        if (!hasConstraintWeight) {
            // Branch only possible using the deprecated ConstraintConfiguration and penalizeConfigurable(...).
            if (solution == null) {
                /*
                 * In constraint verifier API, we allow for testing constraint providers without having a planning solution.
                 * However, constraint weights may be using ConstraintConfiguration
                 * and in that case the solution is required to read the weights from.
                 * For these cases, we set the constraint weight to the softest possible value, just to make sure that the
                 * constraint is not ignored.
                 * The actual value is not used in any way.
                 */
                return (Score_) solutionDescriptor.getScoreDefinition().getOneSoftestScore();
            } else if (hasConstraintWeightSupplier) { // Legacy constraint configuration.
                return constraintWeightSupplier.getConstraintWeight(constraintRef, solution);
            } else {
                throw new UnsupportedOperationException("Impossible state: no %s for constraint (%s)."
                        .formatted(ConstraintConfiguration.class.getSimpleName(), constraintRef));
            }
        } else { // Overridable constraint weight using ConstraintWeights.
            if (hasConstraintWeightSupplier) {
                var weight = constraintWeightSupplier.getConstraintWeight(constraintRef, solution);
                if (weight != null) {
                    return weight;
                }
            }
            AbstractConstraint.validateWeight(solutionDescriptor, constraintRef, (Score_) defaultConstraintWeight);
            return (Score_) defaultConstraintWeight;
        }
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

    @Override
    public String getDescription() {
        return description;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Score<?> getDefaultConstraintWeight() {
        if (defaultConstraintWeight == null) { // Configurable weights (deprecated) have no default.
            return null;
        }
        return adjustConstraintWeight((Score) defaultConstraintWeight);
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

    public static <Solution_, Score_ extends Score<Score_>> void validateWeight(
            SolutionDescriptor<Solution_> solutionDescriptor, ConstraintRef constraintRef, Score_ constraintWeight) {
        if (constraintWeight == null) {
            throw new IllegalArgumentException("""
                    The constraintWeight (null) for constraint (%s) must not be null.
                    Maybe check your constraint implementation."""
                    .formatted(constraintRef));
        }
        var scoreDescriptor = solutionDescriptor.<Score_> getScoreDescriptor();
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
