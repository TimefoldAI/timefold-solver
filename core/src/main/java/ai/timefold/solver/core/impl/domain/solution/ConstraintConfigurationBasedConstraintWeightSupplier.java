package ai.timefold.solver.core.impl.domain.solution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.AbstractBendableScoreDefinition;

/**
 * @deprecated Use {@link ConstraintWeightOverrides} instead.
 */
@Deprecated(forRemoval = true, since = "1.13.0")
public final class ConstraintConfigurationBasedConstraintWeightSupplier<Score_ extends Score<Score_>, Solution_>
        implements ConstraintWeightSupplier<Solution_, Score_> {

    public static <Solution_, Score_ extends Score<Score_>> ConstraintWeightSupplier<Solution_, Score_> create(
            SolutionDescriptor<Solution_> solutionDescriptor, Class<?> constraintConfigurationClass) {
        var configDescriptor = new ConstraintConfigurationDescriptor<>(Objects.requireNonNull(solutionDescriptor),
                Objects.requireNonNull(constraintConfigurationClass));
        return new ConstraintConfigurationBasedConstraintWeightSupplier<>(configDescriptor);
    }

    private final ConstraintConfigurationDescriptor<Solution_> constraintConfigurationDescriptor;
    private final Map<ConstraintRef, Function<Solution_, Score_>> constraintWeightExtractorMap = new LinkedHashMap<>();

    private ConstraintConfigurationBasedConstraintWeightSupplier(
            ConstraintConfigurationDescriptor<Solution_> constraintConfigurationDescriptor) {
        this.constraintConfigurationDescriptor = Objects.requireNonNull(constraintConfigurationDescriptor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(SolutionDescriptor<Solution_> solutionDescriptor, MemberAccessorFactory memberAccessorFactory,
            DomainAccessType domainAccessType) {
        var scoreDescriptor = solutionDescriptor.<Score_> getScoreDescriptor();
        constraintConfigurationDescriptor.processAnnotations(memberAccessorFactory, domainAccessType,
                scoreDescriptor.getScoreDefinition());
        constraintConfigurationDescriptor.getSupportedConstraints().forEach(constraintRef -> {
            var descriptor = constraintConfigurationDescriptor.findConstraintWeightDescriptor(constraintRef);
            var weightExtractor = (Function<Solution_, Score_>) descriptor
                    .createExtractor(solutionDescriptor.getConstraintConfigurationMemberAccessor());
            constraintWeightExtractorMap.put(constraintRef, weightExtractor);
        });
    }

    @Override
    public void validate(Solution_ workingSolution, Set<ConstraintRef> userDefinedConstraints) {
        var missingConstraints = userDefinedConstraints.stream()
                .filter(constraintRef -> !constraintWeightExtractorMap.containsKey(constraintRef))
                .collect(Collectors.toSet());
        if (!missingConstraints.isEmpty()) {
            throw new IllegalStateException("""
                    The constraintConfigurationClass (%s) does not support the following constraints (%s).
                    Maybe ensure your constraint configuration contains all constraints defined in your %s."""
                    .formatted(constraintConfigurationDescriptor.getConstraintConfigurationClass(), missingConstraints,
                            ConstraintProvider.class.getSimpleName()));
        }
        // For backward compatibility reasons, we do not check for excess constraints.
    }

    @Override
    public Class<?> getProblemFactClass() {
        return constraintConfigurationDescriptor.getConstraintConfigurationClass();
    }

    @Override
    public String getDefaultConstraintPackage() {
        return constraintConfigurationDescriptor.getConstraintPackage();
    }

    @Override
    public Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution) {
        var weightExtractor = constraintWeightExtractorMap.get(constraintRef);
        if (weightExtractor == null) { // Should have been caught by validate(...).
            throw new IllegalStateException(
                    "Impossible state: Constraint (%s) not supported by constraint configuration class (%s)."
                            .formatted(constraintRef, constraintConfigurationDescriptor.getConstraintConfigurationClass()));
        }
        var weight = weightExtractor.apply(workingSolution);
        validateConstraintWeight(constraintRef, weight);
        return weight;
    }

    private void validateConstraintWeight(ConstraintRef constraintRef, Score_ constraintWeight) {
        if (constraintWeight == null) {
            throw new IllegalArgumentException("""
                    The constraintWeight for constraint (%s) must not be null.
                    Maybe validate the data input of your constraintConfigurationClass (%s) for that constraint."""
                    .formatted(constraintRef, constraintConfigurationDescriptor.getConstraintConfigurationClass()));
        }
        var scoreDescriptor = constraintConfigurationDescriptor.getSolutionDescriptor().<Score_> getScoreDescriptor();
        if (!scoreDescriptor.getScoreClass().isAssignableFrom(constraintWeight.getClass())) {
            throw new IllegalArgumentException("""
                    The constraintWeight (%s) of class (%s) for constraint (%s) must be of the scoreClass (%s).
                    Maybe validate the data input of your constraintConfigurationClass (%s) for that constraint."""
                    .formatted(constraintWeight, constraintWeight.getClass(), constraintRef, scoreDescriptor.getScoreClass(),
                            constraintConfigurationDescriptor.getConstraintConfigurationClass()));
        }
        if (constraintWeight.initScore() != 0) {
            throw new IllegalArgumentException("""
                    The constraintWeight (%s) for constraint (%s) must have an initScore (%d) equal to 0.
                    Maybe validate the data input of your constraintConfigurationClass (%s) for that constraint."""
                    .formatted(constraintWeight, constraintRef, constraintWeight.initScore(),
                            constraintConfigurationDescriptor.getConstraintConfigurationClass()));
        }
        if (!scoreDescriptor.getScoreDefinition().isPositiveOrZero(constraintWeight)) {
            throw new IllegalArgumentException("""
                    The constraintWeight (%s) for constraint (%s) must be positive or zero.
                    Maybe validate the data input of your constraintConfigurationClass (%s)."""
                    .formatted(constraintWeight, constraintRef,
                            constraintConfigurationDescriptor.getConstraintConfigurationClass()));
        }
        if (constraintWeight instanceof IBendableScore<?> bendableConstraintWeight) {
            var bendableScoreDefinition = (AbstractBendableScoreDefinition<?>) scoreDescriptor.getScoreDefinition();
            if (bendableConstraintWeight.hardLevelsSize() != bendableScoreDefinition.getHardLevelsSize()
                    || bendableConstraintWeight.softLevelsSize() != bendableScoreDefinition.getSoftLevelsSize()) {
                throw new IllegalArgumentException(
                        """
                                The bendable constraintWeight (%s) for constraint (%s) has a hardLevelsSize (%d) or a softLevelsSize (%d) \
                                that doesn't match the score definition's hardLevelsSize (%d) or softLevelsSize (%d).
                                Maybe validate the data input of your constraintConfigurationClass (%s)."""
                                .formatted(bendableConstraintWeight, constraintRef, bendableConstraintWeight.hardLevelsSize(),
                                        bendableConstraintWeight.softLevelsSize(), bendableScoreDefinition.getHardLevelsSize(),
                                        bendableScoreDefinition.getSoftLevelsSize(),
                                        constraintConfigurationDescriptor.getConstraintConfigurationClass()));
            }
        }
    }

    ConstraintConfigurationDescriptor<Solution_> getConstraintConfigurationDescriptor() {
        return constraintConfigurationDescriptor;
    }

    @Override
    public String toString() {
        return "Constraint weights based on " + constraintConfigurationDescriptor.getConstraintConfigurationClass() + ".";
    }

}
