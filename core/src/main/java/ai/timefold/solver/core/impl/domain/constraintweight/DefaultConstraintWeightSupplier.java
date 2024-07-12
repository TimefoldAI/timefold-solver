package ai.timefold.solver.core.impl.domain.constraintweight;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeights;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.score.descriptor.ScoreDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

public final class DefaultConstraintWeightSupplier<Score_ extends Score<Score_>, Solution_>
        implements ConstraintWeightSupplier<Solution_, Score_> {

    public static <Solution_, Score_ extends Score<Score_>> ConstraintWeightSupplier<Solution_, Score_> create(
            SolutionDescriptor<Solution_> solutionDescriptor,
            Class<? extends ConstraintWeights<Score_>> constraintWeightsClass) {
        return new DefaultConstraintWeightSupplier<>(Objects.requireNonNull(solutionDescriptor),
                Objects.requireNonNull(constraintWeightsClass));
    }

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Class<? extends ConstraintWeights<Score_>> constraintWeightsClass;

    private DefaultConstraintWeightSupplier(SolutionDescriptor<Solution_> solutionDescriptor,
            Class<? extends ConstraintWeights<Score_>> constraintWeightsClass) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.constraintWeightsClass = Objects.requireNonNull(constraintWeightsClass);
    }

    @Override
    public void initialize(MemberAccessorFactory memberAccessorFactory, DomainAccessType domainAccessType,
            ScoreDescriptor<Score_> scoreDescriptor) {

    }

    @Override
    public void validate(Solution_ workingSolution, Set<ConstraintRef> userDefinedConstraints) {
        var supportedConstraints = getConstraintWeights(workingSolution).getKnownConstraints();
        var excessiveConstraints = supportedConstraints.stream()
                .filter(constraintRef -> !userDefinedConstraints.contains(constraintRef))
                .collect(Collectors.toSet());
        if (!excessiveConstraints.isEmpty()) {
            throw new IllegalStateException("""
                    The constraintWeightsClass (%s) knows the following constraints (%s) \
                    that are not in the user-defined constraints (%s).
                    Maybe check your %s for missing constraints."""
                    .formatted(constraintWeightsClass, excessiveConstraints, userDefinedConstraints,
                            ConstraintProvider.class.getSimpleName()));
        }
        // Constraints are allowed to be missing; the default value provided by the ConstraintProvider will be used.
    }

    private ConstraintWeights<Score_> getConstraintWeights(Solution_ workingSolution) {
        return null;
    }

    @Override
    public Class<?> getProblemFactClass() {
        return constraintWeightsClass;
    }

    @Override
    public String getDefaultConstraintPackage() {
        return solutionDescriptor.getSolutionClass().getPackageName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution) {
        var weight = (Score_) getConstraintWeights(workingSolution).getConstraintWeight(constraintRef);
        if (weight == null) { // This is fine; use default value from ConstraintProvider.
            return null;
        }
        AbstractConstraint.validateWeight(solutionDescriptor, constraintRef, weight);
        return weight;
    }

}
