package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetForEachUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.ForEachFilteringCriteria;
import ai.timefold.solver.core.impl.score.stream.common.InnerConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

import org.jspecify.annotations.NonNull;

public final class BavetConstraintFactory<Solution_>
        extends InnerConstraintFactory<Solution_, BavetConstraint<Solution_>> {

    /**
     * Used for code in no package, also called the "unnamed package".
     * Classes here can only be instantiated via reflection,
     * they cannot be imported and used directly.
     * But still, in corner cases such as Kotlin notebooks,
     * all code is in the unnamed package.
     * Assume a constraint provider under these conditions,
     * where asConstraint(...) only specifies constraint name, not constraint package.
     * In this situation, the default constraint package is used.
     */
    private static final String DEFAULT_CONSTRAINT_PACKAGE = "unnamed.package";

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final EnvironmentMode environmentMode;
    private final String defaultConstraintPackage;

    private final Map<BavetAbstractConstraintStream<Solution_>, BavetAbstractConstraintStream<Solution_>> sharingStreamMap =
            new HashMap<>(256);

    public BavetConstraintFactory(SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        this.solutionDescriptor = solutionDescriptor;
        this.environmentMode = Objects.requireNonNull(environmentMode);
        var weightSupplier = solutionDescriptor.getConstraintWeightSupplier();
        if (weightSupplier == null) {
            defaultConstraintPackage = determineDefaultConstraintPackage(solutionDescriptor.getSolutionClass().getPackage());
        } else {
            defaultConstraintPackage = determineDefaultConstraintPackage(weightSupplier.getDefaultConstraintPackage());
        }
    }

    private static String determineDefaultConstraintPackage(Package pkg) {
        var asString = pkg == null ? "" : pkg.getName();
        return determineDefaultConstraintPackage(asString);
    }

    private static String determineDefaultConstraintPackage(String constraintPackage) {
        return constraintPackage == null || constraintPackage.isEmpty() ? DEFAULT_CONSTRAINT_PACKAGE : constraintPackage;
    }

    public <Stream_ extends BavetAbstractConstraintStream<Solution_>> Stream_ share(Stream_ stream) {
        return share(stream, t -> {
        });
    }

    /**
     * Enables node sharing.
     * If a constraint already exists in this factory, it replaces it with the old copy.
     * {@link BavetAbstractConstraintStream} implement equals/hashcode ignoring child streams.
     * <p>
     * {@link BavetConstraintSessionFactory#buildSession(Object, ConsistencyTracker, ConstraintMatchPolicy, boolean, Consumer)}
     * needs
     * this to happen
     * for all streams.
     * <p>
     * This must be called before the stream receives child streams.
     *
     * @param stream never null
     * @param consumer never null
     * @param <Stream_> the {@link BavetAbstractConstraintStream} subclass
     * @return never null
     */
    public <Stream_ extends BavetAbstractConstraintStream<Solution_>> Stream_ share(Stream_ stream,
            Consumer<Stream_> consumer) {
        return (Stream_) sharingStreamMap.computeIfAbsent(stream, k -> {
            consumer.accept(stream);
            return stream;
        });
    }

    // ************************************************************************
    // from
    // ************************************************************************
    // Required for node sharing, since using a lambda will create different instances
    private record ForEachFilteringCriteriaPredicateFunction<Solution_, A>(EntityDescriptor<Solution_> entityDescriptor,
            ForEachFilteringCriteria criteria) implements Function<ConstraintNodeBuildHelper<Solution_, ?>, Predicate<A>> {
        public Predicate<A> apply(@NonNull ConstraintNodeBuildHelper<Solution_, ?> helper) {
            return helper.getForEachPredicateForEntityDescriptorAndCriteria(entityDescriptor, criteria);
        }
    }

    private <A> @NonNull UniConstraintStream<A> forEachForCriteria(@NonNull Class<A> sourceClass,
            ForEachFilteringCriteria criteria) {
        assertValidFromType(sourceClass);
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(sourceClass);
        if (entityDescriptor == null || criteria == ForEachFilteringCriteria.ALL) {
            // Not genuine or shadow entity, or filtering was not requested; no need for filtering.
            return share(new BavetForEachUniConstraintStream<>(this, sourceClass, null, RetrievalSemantics.STANDARD));
        }
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null || !listVariableDescriptor.acceptsValueType(sourceClass)) {
            // No applicable list variable; don't need to check inverse relationships.
            return share(new BavetForEachUniConstraintStream<>(this, sourceClass,
                    new ForEachFilteringCriteriaPredicateFunction<>(entityDescriptor, criteria),
                    RetrievalSemantics.STANDARD));
        }
        var entityClass = listVariableDescriptor.getEntityDescriptor().getEntityClass();
        if (entityClass == sourceClass) {
            throw new IllegalStateException("Impossible state: entityClass (%s) and sourceClass (%s) are the same."
                    .formatted(entityClass.getCanonicalName(), sourceClass.getCanonicalName()));
        }
        var shadowDescriptor = listVariableDescriptor.getInverseRelationShadowVariableDescriptor();
        if (shadowDescriptor == null && criteria == ForEachFilteringCriteria.ASSIGNED_AND_CONSISTENT) {
            // The list variable element doesn't have the @InverseRelationShadowVariable annotation,
            // and we are not including unassigned elements.
            // We don't want the users to be forced to implement it in quickstarts,
            // so we'll do this expensive thing instead.
            return forEachForCriteria(sourceClass, ForEachFilteringCriteria.CONSISTENT)
                    .ifExists((Class) entityClass,
                            Joiners.filtering(listVariableDescriptor.getInListPredicate()));
        } else { // We have the inverse relation variable, so we can read its value directly.
            return share(new BavetForEachUniConstraintStream<>(this, sourceClass,
                    new ForEachFilteringCriteriaPredicateFunction<>(entityDescriptor, criteria),
                    RetrievalSemantics.STANDARD));
        }
    }

    @Override
    public <A> @NonNull UniConstraintStream<A> forEach(@NonNull Class<A> sourceClass) {
        return forEachForCriteria(sourceClass, ForEachFilteringCriteria.ASSIGNED_AND_CONSISTENT);
    }

    @Override
    public <A> @NonNull UniConstraintStream<A> forEachIncludingUnassigned(@NonNull Class<A> sourceClass) {
        return forEachForCriteria(sourceClass, ForEachFilteringCriteria.CONSISTENT);
    }

    @Override
    public <A> @NonNull UniConstraintStream<A> forEachUnfiltered(@NonNull Class<A> sourceClass) {
        return forEachForCriteria(sourceClass, ForEachFilteringCriteria.ALL);
    }

    // Required for node sharing, since using a lambda will create different instances
    private record PredicateSupplier<Solution_, A>(
            Predicate<A> suppliedPredicate) implements Function<ConstraintNodeBuildHelper<Solution_, ?>, Predicate<A>> {
        public Predicate<A> apply(@NonNull ConstraintNodeBuildHelper<Solution_, ?> helper) {
            return suppliedPredicate;
        }
    }

    @Override
    public @NonNull <A> UniConstraintStream<A> from(@NonNull Class<A> fromClass) {
        assertValidFromType(fromClass);
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(fromClass);
        if (entityDescriptor != null && entityDescriptor.isGenuine()) {
            var predicate = (Predicate<A>) entityDescriptor.getIsInitializedPredicate();
            return share(
                    new BavetForEachUniConstraintStream<>(this, fromClass, new PredicateSupplier<>(predicate),
                            RetrievalSemantics.LEGACY));
        } else {
            return share(new BavetForEachUniConstraintStream<>(this, fromClass, null, RetrievalSemantics.LEGACY));
        }
    }

    @Override
    public @NonNull <A> UniConstraintStream<A> fromUnfiltered(@NonNull Class<A> fromClass) {
        assertValidFromType(fromClass);
        return share(new BavetForEachUniConstraintStream<>(this, fromClass, null, RetrievalSemantics.LEGACY));
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    public EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    @Override
    public @NonNull String getDefaultConstraintPackage() {
        return defaultConstraintPackage;
    }

}
