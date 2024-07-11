package ai.timefold.solver.core.impl.domain.constraintweight;

import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeights;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
class ConstraintWeightsDescriptor<Solution_, Score_ extends Score<Score_>> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;

    private final Class<? extends ConstraintWeights<Score_>> constraintWeightsClass;
    private String constraintPackage;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ConstraintWeightsDescriptor(SolutionDescriptor<Solution_> solutionDescriptor,
            Class<? extends ConstraintWeights<Score_>> constraintWeightsClass) {
        this.solutionDescriptor = solutionDescriptor;
        this.constraintWeightsClass = constraintWeightsClass;
    }

    public String getConstraintPackage() {
        return constraintPackage;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void processAnnotations(DescriptorPolicy descriptorPolicy, ScoreDefinition<Score_> scoreDefinition) {

    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    public Class<?> getConstraintWeightsClass() {
        return constraintWeightsClass;
    }

    public Set<ConstraintRef> getSupportedConstraints() {
        return null;
    }

    public ConstraintWeightDescriptor<Solution_> findConstraintWeightDescriptor(ConstraintRef constraintRef) {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + constraintWeightsClass.getName() + ")";
    }
}
