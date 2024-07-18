package ai.timefold.solver.core.impl.domain.solution;

import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public sealed interface ConstraintWeightSupplier<Solution_, Score_ extends Score<Score_>>
        permits OverridesBasedConstraintWeightSupplier, ConstraintConfigurationBasedConstraintWeightSupplier {

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

    /**
     * The class that carries the constraint weights.
     * It will either be annotated by {@link ConstraintConfiguration},
     * or be {@link ConstraintWeightOverrides}.
     *
     * @return never null
     */
    Class<?> getProblemFactClass();

    String getDefaultConstraintPackage();

    /**
     * Get the weight for the constraint if known to the supplier.
     * Supplies may choose not to provide a value for unknown constraints,
     * which is the case for {@link OverridesBasedConstraintWeightSupplier}.
     * {@link ConstraintConfigurationBasedConstraintWeightSupplier} will always provide a value.
     *
     * @param constraintRef never null
     * @param workingSolution never null
     * @return may be null, if the provider does not know the constraint
     */
    Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution);

}
