package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.bi.BavetAbstractBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.bi.BavetPrecomputeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetAbstractQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetPrecomputeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetAbstractTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetPrecomputeTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetForEachUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetPrecomputeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.ForEachFilteringCriteria;
import ai.timefold.solver.core.impl.score.stream.common.InnerConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
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

    private static String determineDefaultConstraintPackage(@Nullable Package pkg) {
        var asString = pkg == null ? "" : pkg.getName();
        return determineDefaultConstraintPackage(asString);
    }

    private static String determineDefaultConstraintPackage(@Nullable String constraintPackage) {
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
        public Predicate<A> apply(ConstraintNodeBuildHelper<Solution_, ?> helper) {
            return helper.getForEachPredicateForEntityDescriptorAndCriteria(entityDescriptor, criteria);
        }
    }

    private <A> UniConstraintStream<A> forEachForCriteria(Class<A> sourceClass,
            ForEachFilteringCriteria criteria) {
        return forEachForCriteria(sourceClass, criteria, RetrievalSemantics.STANDARD);
    }

    private <A> UniConstraintStream<A> forEachForCriteria(Class<A> sourceClass,
            ForEachFilteringCriteria criteria, RetrievalSemantics retrievalSemantics) {
        assertValidFromType(sourceClass);
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(sourceClass);
        if (entityDescriptor == null || criteria == ForEachFilteringCriteria.ALL) {
            // Not genuine or shadow entity, or filtering was not requested; no need for filtering.
            return share(new BavetForEachUniConstraintStream<>(this, sourceClass, null, retrievalSemantics));
        }
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null || !listVariableDescriptor.acceptsValueType(sourceClass)) {
            // No applicable list variable; don't need to check inverse relationships.
            return share(new BavetForEachUniConstraintStream<>(this, sourceClass,
                    new ForEachFilteringCriteriaPredicateFunction<>(entityDescriptor, criteria),
                    retrievalSemantics));
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
                    retrievalSemantics));
        }
    }

    @Override
    public <A> UniConstraintStream<A> forEach(Class<A> sourceClass) {
        return forEachForCriteria(sourceClass, ForEachFilteringCriteria.ASSIGNED_AND_CONSISTENT);
    }

    @Override
    public <A> UniConstraintStream<A> forEachIncludingUnassigned(Class<A> sourceClass) {
        return forEachForCriteria(sourceClass, ForEachFilteringCriteria.CONSISTENT);
    }

    @Override
    public <A> UniConstraintStream<A> forEachUnfiltered(Class<A> sourceClass) {
        return forEachForCriteria(sourceClass, ForEachFilteringCriteria.ALL);
    }

    <A> UniConstraintStream<A> forEachUnfilteredStatic(Class<A> sourceClass) {
        return forEachForCriteria(sourceClass, ForEachFilteringCriteria.ALL, RetrievalSemantics.PRECOMPUTE);
    }

    // Required for node sharing, since using a lambda will create different instances
    private record PredicateSupplier<Solution_, A>(
            Predicate<A> suppliedPredicate) implements Function<ConstraintNodeBuildHelper<Solution_, ?>, Predicate<A>> {
        public Predicate<A> apply(ConstraintNodeBuildHelper<Solution_, ?> helper) {
            return suppliedPredicate;
        }
    }

    @Override
    public <A> UniConstraintStream<A> from(Class<A> fromClass) {
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
    @SuppressWarnings("unchecked")
    public <Stream_ extends ConstraintStream> Stream_
            precompute(Function<PrecomputeFactory, Stream_> precomputeSupplier) {
        var bavetStream = Objects.requireNonNull(precomputeSupplier.apply(new BavetStaticDataFactory<>(this)));
        // TODO: Use switch here in JDK 21
        if (bavetStream instanceof BavetAbstractUniConstraintStream<?, ?> uniStream) {
            var out = new BavetPrecomputeUniConstraintStream<>(this,
                    (BavetAbstractUniConstraintStream<Solution_, ?>) uniStream);
            return (Stream_) share(new BavetAftBridgeUniConstraintStream<>(this, out),
                    out::setAftBridge);
        } else if (bavetStream instanceof BavetAbstractBiConstraintStream<?, ?, ?> biStream) {
            var out = new BavetPrecomputeBiConstraintStream<>(this,
                    (BavetAbstractBiConstraintStream<Solution_, ?, ?>) biStream);
            return (Stream_) share(new BavetAftBridgeBiConstraintStream<>(this, out),
                    out::setAftBridge);
        } else if (bavetStream instanceof BavetAbstractTriConstraintStream<?, ?, ?, ?> triStream) {
            var out = new BavetPrecomputeTriConstraintStream<>(this,
                    (BavetAbstractTriConstraintStream<Solution_, ?, ?, ?>) triStream);
            return (Stream_) share(new BavetAftBridgeTriConstraintStream<>(this, out),
                    out::setAftBridge);
        } else if (bavetStream instanceof BavetAbstractQuadConstraintStream<?, ?, ?, ?, ?> quadStream) {
            var out = new BavetPrecomputeQuadConstraintStream<>(this,
                    (BavetAbstractQuadConstraintStream<Solution_, ?, ?, ?, ?>) quadStream);
            return (Stream_) share(new BavetAftBridgeQuadConstraintStream<>(this, out),
                    out::setAftBridge);
        } else {
            throw new IllegalStateException(
                    "impossible state: the supplier (%s) returned a stream (%s) that not an instance of any Bavet ConstraintStream"
                            .formatted(precomputeSupplier, bavetStream));
        }
    }

    @Override
    public <A> UniConstraintStream<A> fromUnfiltered(Class<A> fromClass) {
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
    public String getDefaultConstraintPackage() {
        return defaultConstraintPackage;
    }

}
