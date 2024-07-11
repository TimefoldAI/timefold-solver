package ai.timefold.solver.core.impl.domain.constraintweight;

import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.score.descriptor.ScoreDescriptor;

public sealed interface ConstraintWeightSupplier<Solution_, Score_ extends Score<Score_>>
        permits DefaultConstraintWeightSupplier, LegacyConstraintWeightSupplier {

    void initialize(MemberAccessorFactory memberAccessorFactory, DomainAccessType domainAccessType,
            ScoreDescriptor<Score_> scoreDescriptor);

    Class<?> getProblemFactClass();

    String getDefaultConstraintPackage();

    Set<ConstraintRef> getSupportedConstraints();

    Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution);

    void validateConstraintWeight(ConstraintRef constraintRef, Score_ constraintWeight);

}
