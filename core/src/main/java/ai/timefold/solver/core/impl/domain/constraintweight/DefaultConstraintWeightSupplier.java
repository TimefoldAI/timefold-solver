package ai.timefold.solver.core.impl.domain.constraintweight;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeights;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
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
    public Class<?> getProblemFactClass() {
        return constraintWeightsClass;
    }

    @Override
    public String getDefaultConstraintPackage() {
        return solutionDescriptor.getSolutionClass().getPackageName();
    }

    @Override
    public Set<ConstraintRef> getSupportedConstraints() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateConstraintWeight(ConstraintRef constraintRef, Score_ constraintWeight) {
        AbstractConstraint.validateWeight(solutionDescriptor, constraintRef, constraintWeight);
    }

}
