package ai.timefold.solver.core.impl.score.stream.common;

import static ai.timefold.solver.core.api.score.stream.Joiners.lessThan;
import static java.util.stream.Collectors.groupingBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.impl.bavet.bi.joiner.BiJoinerComber;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.stream.common.uni.InnerUniConstraintStream;

import org.jspecify.annotations.NonNull;

public abstract class InnerConstraintFactory<Solution_, Constraint_ extends Constraint> implements ConstraintFactory {

    @Override
    public <A> @NonNull BiConstraintStream<A, A> forEachUniquePair(@NonNull Class<A> sourceClass,
            BiJoiner<A, A> @NonNull... joiners) {
        BiJoinerComber<A, A> joinerComber = BiJoinerComber.comb(joiners);
        joinerComber.addJoiner(buildLessThanId(sourceClass));
        return ((InnerUniConstraintStream<A>) forEach(sourceClass))
                .join(forEach(sourceClass), joinerComber);
    }

    private <A> DefaultBiJoiner<A, A> buildLessThanId(Class<A> sourceClass) {
        SolutionDescriptor<Solution_> solutionDescriptor = getSolutionDescriptor();
        MemberAccessor planningIdMemberAccessor = solutionDescriptor.getPlanningIdAccessor(sourceClass);
        if (planningIdMemberAccessor == null) {
            throw new IllegalArgumentException(
                    "The fromClass (%s) has no member with a @%s annotation, so the pairs cannot be made unique ([A,B] vs [B,A])."
                            .formatted(sourceClass, PlanningId.class.getSimpleName()));
        }
        Function<A, Comparable> planningIdGetter = planningIdMemberAccessor.getGetterFunction();
        return (DefaultBiJoiner<A, A>) lessThan(planningIdGetter);
    }

    @Override
    public @NonNull <A> BiConstraintStream<A, A> fromUniquePair(@NonNull Class<A> fromClass,
            @NonNull BiJoiner<A, A>... joiners) {
        BiJoinerComber<A, A> joinerComber = BiJoinerComber.comb(joiners);
        joinerComber.addJoiner(buildLessThanId(fromClass));
        return ((InnerUniConstraintStream<A>) from(fromClass))
                .join(from(fromClass), joinerComber);
    }

    public <A> void assertValidFromType(Class<A> fromType) {
        SolutionDescriptor<Solution_> solutionDescriptor = getSolutionDescriptor();
        Set<Class<?>> problemFactOrEntityClassSet = solutionDescriptor.getProblemFactOrEntityClassSet();
        /*
         * Need to support the following situations:
         * 1/ FactType == FromType; querying for the declared type.
         * 2/ FromType extends/implements FactType; querying for impl type where declared type is its interface.
         * 3/ FromType super FactType; querying for interface where declared type is its implementation.
         */
        boolean hasMatchingType = problemFactOrEntityClassSet.stream()
                .anyMatch(factType -> fromType.isAssignableFrom(factType) || factType.isAssignableFrom(fromType));
        if (!hasMatchingType) {
            List<String> canonicalClassNameList = problemFactOrEntityClassSet.stream()
                    .map(Class::getCanonicalName)
                    .sorted()
                    .toList();
            throw new IllegalArgumentException("""
                    Cannot use class (%s) in a constraint stream as it is neither the same as, \
                    nor a superclass or superinterface of one of planning entities or problem facts.
                    Ensure that all from(), join(), ifExists() and ifNotExists() building blocks \
                    only reference classes assignable from planning entities \
                    or problem facts (%s) annotated on the planning solution (%s)."""
                    .formatted(fromType.getCanonicalName(), canonicalClassNameList,
                            solutionDescriptor.getSolutionClass().getCanonicalName()));
        }
    }

    @SuppressWarnings("unchecked")
    public List<Constraint_> buildConstraints(ConstraintProvider constraintProvider) {
        Constraint[] constraints = Objects.requireNonNull(constraintProvider.defineConstraints(this),
                () -> """
                        The constraintProvider class (%s)'s defineConstraints() must not return null."
                        Maybe return an empty array instead if there are no constraints."""
                        .formatted(constraintProvider.getClass()));
        if (Arrays.stream(constraints).anyMatch(Objects::isNull)) {
            throw new IllegalStateException("""
                    The constraintProvider class (%s)'s defineConstraints() must not contain an element that is null.
                    Maybe don't include any null elements in the %s array."""
                    .formatted(constraintProvider.getClass(), Constraint.class.getSimpleName()));
        }
        // Fail fast on duplicate constraint IDs.
        Map<ConstraintRef, List<Constraint>> constraintsPerIdMap =
                Arrays.stream(constraints).collect(groupingBy(Constraint::getConstraintRef));
        constraintsPerIdMap.forEach((constraintRef, duplicateConstraintList) -> {
            if (duplicateConstraintList.size() > 1) {
                throw new IllegalStateException("There are multiple constraints with the same ID (%s)."
                        .formatted(constraintRef));
            }
        });
        return Arrays.stream(constraints)
                .map(c -> (Constraint_) c)
                .collect(Collectors.toList());
    }

    /**
     * @return never null
     */
    public abstract SolutionDescriptor<Solution_> getSolutionDescriptor();

}
