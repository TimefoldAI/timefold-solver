package ai.timefold.solver.core.impl.domain.solution;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

public final class OverridesBasedConstraintWeightSupplier<Score_ extends Score<Score_>, Solution_>
        implements ConstraintWeightSupplier<Solution_, Score_> {

    public static <Solution_, Score_ extends Score<Score_>> ConstraintWeightSupplier<Solution_, Score_> create(
            SolutionDescriptor<Solution_> solutionDescriptor, DescriptorPolicy descriptorPolicy,
            Field field) {
        var method = ReflectionHelper.getGetterMethod(field.getDeclaringClass(), field.getName());
        Class<? extends ConstraintWeightOverrides<Score_>> overridesClass;
        Member member;

        // Prefer method to field
        // (In Python, the field doesn't implement the interface).
        if (method == null) {
            member = field;
            overridesClass = (Class<? extends ConstraintWeightOverrides<Score_>>) field.getType();
        } else {
            member = method;
            overridesClass = (Class<? extends ConstraintWeightOverrides<Score_>>) method.getReturnType();
        }
        var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                descriptorPolicy.getDomainAccessType());
        return new OverridesBasedConstraintWeightSupplier<>(solutionDescriptor, memberAccessor, overridesClass);
    }

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final MemberAccessor overridesAccessor;
    private final Class<? extends ConstraintWeightOverrides<Score_>> overridesClass;

    private OverridesBasedConstraintWeightSupplier(SolutionDescriptor<Solution_> solutionDescriptor,
            MemberAccessor overridesAccessor, Class<? extends ConstraintWeightOverrides<Score_>> overridesClass) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.overridesAccessor = Objects.requireNonNull(overridesAccessor);
        this.overridesClass = Objects.requireNonNull(overridesClass);
    }

    @Override
    public void initialize(SolutionDescriptor<Solution_> solutionDescriptor, MemberAccessorFactory memberAccessorFactory,
            DomainAccessType domainAccessType) {
        // No need to do anything.
    }

    @Override
    public void validate(Solution_ workingSolution, Set<ConstraintRef> userDefinedConstraints) {
        var userDefinedConstraintNames = userDefinedConstraints.stream()
                .map(ConstraintRef::constraintName)
                .collect(Collectors.toSet());
        // Constraint verifier is known to cause null here.
        var overrides = workingSolution == null ? ConstraintWeightOverrides.none()
                : Objects.requireNonNull(getConstraintWeights(workingSolution));
        var supportedConstraints = overrides.getKnownConstraintNames();
        var excessiveConstraints = supportedConstraints.stream()
                .filter(constraintName -> !userDefinedConstraintNames.contains(constraintName))
                .collect(Collectors.toSet());
        if (!excessiveConstraints.isEmpty()) {
            throw new IllegalStateException("""
                    The constraint weight overrides contain the following constraints (%s) \
                    that are not in the user-defined constraints (%s).
                    Maybe check your %s for missing constraints."""
                    .formatted(excessiveConstraints, userDefinedConstraintNames,
                            ConstraintProvider.class.getSimpleName()));
        }
        // Constraints are allowed to be missing; the default value provided by the ConstraintProvider will be used.
    }

    @SuppressWarnings("unchecked")
    private ConstraintWeightOverrides<Score_> getConstraintWeights(Solution_ workingSolution) {
        return (ConstraintWeightOverrides<Score_>) overridesAccessor.executeGetter(workingSolution);
    }

    @Override
    public Class<?> getProblemFactClass() {
        return overridesClass;
    }

    @Override
    public String getDefaultConstraintPackage() {
        return solutionDescriptor.getSolutionClass().getPackageName();
    }

    @Override
    public Score_ getConstraintWeight(ConstraintRef constraintRef, Solution_ workingSolution) {
        if (!constraintRef.packageName().equals(getDefaultConstraintPackage())) {
            throw new IllegalStateException("""
                    The constraint (%s) is not in the default package (%s).
                    Constraint packages are deprecated, check your constraint implementation."""
                    .formatted(constraintRef, getDefaultConstraintPackage()));
        }
        if (workingSolution == null) { // ConstraintVerifier is known to cause null here.
            return null;
        }
        var weight = (Score_) getConstraintWeights(workingSolution).getConstraintWeight(constraintRef.constraintName());
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
