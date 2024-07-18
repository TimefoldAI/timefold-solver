package ai.timefold.solver.core.impl.domain.solution;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @deprecated Use {@link ConstraintWeightOverrides} instead.
 */
@Deprecated(forRemoval = true, since = "1.13.0")
final class ConstraintWeightDescriptor<Solution_> {

    private final ConstraintRef constraintRef;
    private final MemberAccessor memberAccessor;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ConstraintWeightDescriptor(MemberAccessor memberAccessor) {
        ConstraintWeight constraintWeightAnnotation = memberAccessor.getAnnotation(ConstraintWeight.class);
        String constraintPackage = constraintWeightAnnotation.constraintPackage();
        if (constraintPackage.isEmpty()) {
            // If a @ConstraintConfiguration extends a @ConstraintConfiguration, their constraintPackage might differ.
            ConstraintConfiguration constraintConfigurationAnnotation = memberAccessor.getDeclaringClass()
                    .getAnnotation(ConstraintConfiguration.class);
            if (constraintConfigurationAnnotation == null) {
                throw new IllegalStateException("Impossible state: " + ConstraintConfigurationDescriptor.class.getSimpleName()
                        + " only reflects over members with a @" + ConstraintConfiguration.class.getSimpleName()
                        + " annotation.");
            }
            constraintPackage = constraintConfigurationAnnotation.constraintPackage();
            if (constraintPackage.isEmpty()) {
                Package pack = memberAccessor.getDeclaringClass().getPackage();
                constraintPackage = (pack == null) ? "" : pack.getName();
            }
        }
        this.constraintRef = ConstraintRef.of(constraintPackage, constraintWeightAnnotation.value());
        this.memberAccessor = memberAccessor;
    }

    public ConstraintRef getConstraintRef() {
        return constraintRef;
    }

    public MemberAccessor getMemberAccessor() {
        return memberAccessor;
    }

    public Function<Solution_, Score<?>> createExtractor(MemberAccessor constraintConfigurationMemberAccessor) {
        return (Solution_ solution) -> {
            Object constraintConfiguration = Objects.requireNonNull(
                    constraintConfigurationMemberAccessor.executeGetter(solution),
                    "Constraint configuration provider (" + constraintConfigurationMemberAccessor +
                            ") returns null.");
            return (Score<?>) memberAccessor.executeGetter(constraintConfiguration);
        };
    }

    @Override
    public String toString() {
        return constraintRef.toString();
    }

}
