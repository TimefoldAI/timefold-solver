package ai.timefold.solver.core.impl.domain.solution;

import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public sealed interface ConstraintWeightSupplier<Solution_, Score_ extends Score<Score_>>
        permits ConstraintWeightsBasedConstraintWeightSupplier, ConstraintConfigurationBasedConstraintWeightSupplier {

    void initialize(SolutionDescriptor<Solution_> solutionDescriptor, MemberAccessorFactory memberAccessorFactory,
            DomainAccessType domainAccessType);

    /**
     * Will be called after {@link #initialize(SolutionDescriptor, MemberAccessorFactory, DomainAccessType)}.
     * Has the option of failing fast in case of discrepancies
     * between the constraints defined in {@link ConstraintProvider}
     * and the constraints defined in the configuration.
     * 
     * @param userDefinedConstraints never null
     */
    void validate(Solution_ workingSolution, Set<ConstraintRef> userDefinedConstraints);

    Class<?> getProblemFactClass();

    String getDefaultConstraintPackage();

    Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution);

}
