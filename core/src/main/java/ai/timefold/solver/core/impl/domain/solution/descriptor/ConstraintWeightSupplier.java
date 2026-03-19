package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ConstraintWeightSupplier<Solution_, Score_ extends Score<Score_>> {

    @SuppressWarnings("unchecked")
    public static <Solution_, Score_ extends Score<Score_>> ConstraintWeightSupplier<Solution_, Score_>
            create(SolutionDescriptor<Solution_> solutionDescriptor, DescriptorPolicy descriptorPolicy, Field field) {
        var method = ReflectionHelper.getGetterMethod(field.getDeclaringClass(), field.getName());
        Class<? extends ConstraintWeightOverrides<Score_>> overridesClass;
        Member member;

        // Prefer method to field
        if (method == null) {
            member = field;
            overridesClass = (Class<? extends ConstraintWeightOverrides<Score_>>) field.getType();
        } else {
            member = method;
            overridesClass = (Class<? extends ConstraintWeightOverrides<Score_>>) method.getReturnType();
        }
        var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER, descriptorPolicy.getDomainAccessType());
        return new ConstraintWeightSupplier<>(solutionDescriptor, memberAccessor, overridesClass);
    }

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final MemberAccessor overridesAccessor;
    private final Class<? extends ConstraintWeightOverrides<Score_>> overridesClass;

    private ConstraintWeightSupplier(SolutionDescriptor<Solution_> solutionDescriptor, MemberAccessor overridesAccessor,
            Class<? extends ConstraintWeightOverrides<Score_>> overridesClass) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.overridesAccessor = Objects.requireNonNull(overridesAccessor);
        this.overridesClass = Objects.requireNonNull(overridesClass);
    }

    /**
     * Has the option of failing fast in case of discrepancies
     * between the constraints defined in {@link ConstraintProvider}
     * and the constraints defined in the configuration.
     *
     * @param workingSolution may be null, in which case the supplier will use the default constraint weights
     * @param userDefinedConstraints never null
     */
    public void validate(@Nullable Solution_ workingSolution, Set<ConstraintRef> userDefinedConstraints) {
        var userDefinedConstraintNames =
                userDefinedConstraints.stream().map(ConstraintRef::constraintName).collect(Collectors.toSet());
        // Constraint verifier is known to cause null here.
        var overrides = workingSolution == null ? ConstraintWeightOverrides.none()
                : Objects.requireNonNull(getConstraintWeights(workingSolution));
        var supportedConstraints = overrides.getKnownConstraintNames();
        var excessiveConstraints = supportedConstraints.stream()
                .filter(constraintName -> !userDefinedConstraintNames.contains(constraintName)).collect(Collectors.toSet());
        if (!excessiveConstraints.isEmpty()) {
            throw new IllegalStateException("""
                    The constraint weight overrides contain the following constraints (%s) \
                    that are not in the user-defined constraints (%s).
                    Maybe check your %s for missing constraints.""".formatted(excessiveConstraints, userDefinedConstraintNames,
                    ConstraintProvider.class.getSimpleName()));
        }
        // Constraints are allowed to be missing; the default value provided by the ConstraintProvider will be used.
    }

    @SuppressWarnings("unchecked")
    private ConstraintWeightOverrides<Score_> getConstraintWeights(Solution_ workingSolution) {
        return (ConstraintWeightOverrides<Score_>) overridesAccessor.executeGetter(workingSolution);
    }

    /**
     * The class that carries the constraint weights.
     *
     * @return never null
     */
    public Class<? extends ConstraintWeightOverrides<Score_>> getProblemFactClass() {
        return overridesClass;
    }

    /**
     * Get the weight for the constraint if known to the supplier.
     * Supplies may choose not to provide a value for unknown constraints,
     * which is the case for {@link ConstraintWeightSupplier}.
     *
     * @param constraintRef never null
     * @param workingSolution if null, will return null
     * @return may be null, if the provider does not know the constraint
     */
    public @Nullable Score_ getConstraintWeight(ConstraintRef constraintRef, @Nullable Solution_ workingSolution) {
        if (workingSolution == null) { // ConstraintVerifier is known to cause null here.
            return null;
        }
        var weight = getConstraintWeights(workingSolution).getConstraintWeight(constraintRef.constraintName());
        if (weight == null) { // This is fine; use default value from ConstraintProvider.
            return null;
        }
        AbstractConstraint.validateWeight(solutionDescriptor, constraintRef, weight);
        return weight;
    }

    @Override
    public String toString() {
        return "Constraint weights based on " + overridesAccessor + ".";
    }
}
