package ai.timefold.solver.core.impl.domain.constraintweight;

import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.score.descriptor.ScoreDescriptor;

public sealed interface ConstraintWeightSupplier<Solution_, Score_ extends Score<Score_>>
        permits DefaultConstraintWeightSupplier, LegacyConstraintWeightSupplier {

    void initialize(MemberAccessorFactory memberAccessorFactory, DomainAccessType domainAccessType,
            ScoreDescriptor<Score_> scoreDescriptor);

    /**
     * Will be called after {@link #initialize(MemberAccessorFactory, DomainAccessType, ScoreDescriptor)}.
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
