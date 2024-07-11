package ai.timefold.solver.core.impl.domain.constraintweight;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.score.descriptor.ScoreDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.AbstractBendableScoreDefinition;

public final class LegacyConstraintWeightSupplier<Score_ extends Score<Score_>, Solution_>
        implements ConstraintWeightSupplier<Solution_, Score_> {

    public static <Solution_, Score_ extends Score<Score_>> ConstraintWeightSupplier<Solution_, Score_> create(
            SolutionDescriptor<Solution_> solutionDescriptor, Class<?> constraintConfigurationClass) {
        var configDescriptor = new ConstraintConfigurationDescriptor<>(Objects.requireNonNull(solutionDescriptor),
                Objects.requireNonNull(constraintConfigurationClass));
        return new LegacyConstraintWeightSupplier<>(configDescriptor);
    }

    private final ConstraintConfigurationDescriptor<Solution_> constraintConfigurationDescriptor;
    private final Map<ConstraintRef, Function<Solution_, Score_>> constraintWeightExtractorMap = new LinkedHashMap<>();

    private LegacyConstraintWeightSupplier(ConstraintConfigurationDescriptor<Solution_> constraintConfigurationDescriptor) {
        this.constraintConfigurationDescriptor = Objects.requireNonNull(constraintConfigurationDescriptor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(MemberAccessorFactory memberAccessorFactory, DomainAccessType domainAccessType,
            ScoreDescriptor<Score_> scoreDescriptor) {
        constraintConfigurationDescriptor.processAnnotations(memberAccessorFactory, domainAccessType,
                scoreDescriptor.getScoreDefinition());
        constraintConfigurationDescriptor.getSupportedConstraints().forEach(constraintRef -> {
            var descriptor = constraintConfigurationDescriptor.findConstraintWeightDescriptor(constraintRef);
            var weightExtractor = (Function<Solution_, Score_>) descriptor.createExtractor();
            constraintWeightExtractorMap.put(constraintRef, weightExtractor);
        });
    }

    @Override
    public Class<?> getProblemFactClass() {
        return constraintConfigurationDescriptor.getConstraintConfigurationClass();
    }

    @Override
    public Set<ConstraintRef> getSupportedConstraints() {
        return Collections.unmodifiableSet(constraintWeightExtractorMap.keySet());
    }

    @Override
    public Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution) {
        var weightExtractor = constraintWeightExtractorMap.get(constraintRef);
        if (weightExtractor == null) {
            throw new IllegalStateException("The constraint (%s) is not supported by constraint configuration class (%s)."
                    .formatted(constraintRef, constraintConfigurationDescriptor.getConstraintConfigurationClass()));
        }
        return weightExtractor.apply(workingSolution);
    }

    @Override
    public void validateConstraintWeight(ConstraintRef constraintRef, Score_ constraintWeight) {
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
}
