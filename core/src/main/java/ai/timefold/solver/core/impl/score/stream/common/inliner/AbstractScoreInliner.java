package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;
import ai.timefold.solver.core.impl.score.definition.BendableBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.BendableScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.HardMediumSoftBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.HardMediumSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.HardSoftBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.SimpleBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;

/**
 * Keeps track of the working score and constraint matches for a single constraint session.
 * Every time constraint weights change, a new instance needs to be created.
 *
 * @param <Score_>
 */
public abstract class AbstractScoreInliner<Score_ extends Score<Score_>> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <Score_ extends Score<Score_>, ScoreInliner_ extends AbstractScoreInliner<Score_>> ScoreInliner_
            buildScoreInliner(ScoreDefinition<Score_> scoreDefinition, Map<Constraint, Score_> constraintWeightMap,
                    ConstraintMatchPolicy constraintMatchPolicy) {
        return (ScoreInliner_) switch (scoreDefinition) {
            case SimpleScoreDefinition simpleScoreDefinition ->
                new SimpleScoreInliner((Map) constraintWeightMap, constraintMatchPolicy);
            case SimpleBigDecimalScoreDefinition simpleBigDecimalScoreDefinition ->
                new SimpleBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchPolicy);
            case HardSoftScoreDefinition hardSoftScoreDefinition ->
                new HardSoftScoreInliner((Map) constraintWeightMap, constraintMatchPolicy);
            case HardSoftBigDecimalScoreDefinition hardSoftBigDecimalScoreDefinition ->
                new HardSoftBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchPolicy);
            case HardMediumSoftScoreDefinition hardMediumSoftScoreDefinition ->
                new HardMediumSoftScoreInliner((Map) constraintWeightMap, constraintMatchPolicy);
            case HardMediumSoftBigDecimalScoreDefinition hardMediumSoftBigDecimalScoreDefinition ->
                new HardMediumSoftBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchPolicy);
            case BendableScoreDefinition bendableScoreDefinition ->
                new BendableScoreInliner((Map) constraintWeightMap, constraintMatchPolicy,
                        bendableScoreDefinition.getHardLevelsSize(), bendableScoreDefinition.getSoftLevelsSize());
            case BendableBigDecimalScoreDefinition bendableScoreDefinition ->
                new BendableBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchPolicy,
                        bendableScoreDefinition.getHardLevelsSize(), bendableScoreDefinition.getSoftLevelsSize());
            default -> throw new UnsupportedOperationException("Impossible state: Unknown score definition class (%s)."
                    .formatted(scoreDefinition.getClass().getCanonicalName()));
        };
    }

    protected final ConstraintMatchPolicy constraintMatchPolicy;
    protected final Map<Constraint, Score_> constraintWeightMap;
    private final Map<Constraint, ElementAwareLinkedList<ConstraintMatchCarrier<Score_>>> constraintMatchMap;
    private Map<String, ConstraintMatchTotal<Score_>> constraintIdToConstraintMatchTotalMap = null;
    private Map<Object, Indictment<Score_>> indictmentMap = null;

    protected AbstractScoreInliner(Map<Constraint, Score_> constraintWeightMap, ConstraintMatchPolicy constraintMatchPolicy) {
        this.constraintMatchPolicy = constraintMatchPolicy;
        constraintWeightMap.forEach(this::validateConstraintWeight);
        this.constraintWeightMap = constraintWeightMap;
        if (constraintMatchPolicy.isEnabled()) {
            this.constraintMatchMap = CollectionUtils.newIdentityHashMap(constraintWeightMap.size());
            for (var constraint : constraintWeightMap.keySet()) {
                // Ensure that even constraints without matches have their entry.
                this.constraintMatchMap.put(constraint, new ElementAwareLinkedList<>());
            }
        } else {
            this.constraintMatchMap = Collections.emptyMap();
        }
    }

    private void validateConstraintWeight(Constraint constraint, Score_ constraintWeight) {
        if (constraintWeight == null || constraintWeight.isZero()) {
            throw new IllegalArgumentException("Impossible state: The constraintWeight (" +
                    constraintWeight + ") cannot be zero, constraint (" + constraint +
                    ") should have been culled during session creation.");
        }
    }

    public abstract Score_ extractScore();

    /**
     * Create a new instance of {@link WeightedScoreImpacter} for a particular constraint.
     *
     * @param constraint never null
     * @return never null
     */
    public abstract WeightedScoreImpacter<Score_, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint);

    protected final ScoreImpact<Score_> addConstraintMatch(Constraint constraint,
            ConstraintMatchSupplier<Score_> constraintMatchSupplier, ScoreImpact<Score_> scoreImpact) {
        var constraintMatchList = getConstraintMatchList(constraint);
        /*
         * Creating a constraint match is a heavy operation which may yet be undone.
         * Defer creation of the constraint match until a later point.
         */
        var entry = constraintMatchList.add(new ConstraintMatchCarrier<>(constraintMatchSupplier, constraint, scoreImpact));
        clearMaps();
        return new WrappingScoreImpact<>(this, scoreImpact, entry);
    }

    private record WrappingScoreImpact<Score_ extends Score<Score_>>(AbstractScoreInliner<Score_> inliner,
            ScoreImpact<Score_> delegate,
            ElementAwareLinkedList.Entry<ConstraintMatchCarrier<Score_>> entry)
            implements
                ScoreImpact<Score_> {

        @Override
        public void undo() {
            delegate.undo();
            entry.remove();
            inliner.clearMaps();
        }

        @Override
        public Score_ toScore() {
            return delegate.toScore();
        }

    }

    private ElementAwareLinkedList<ConstraintMatchCarrier<Score_>> getConstraintMatchList(Constraint constraint) {
        // Optimization: computeIfAbsent() would have created a lambda on the hot path.
        var constraintMatchList = constraintMatchMap.get(constraint);
        if (constraintMatchList == null) {
            throw new IllegalStateException(
                    "Impossible state: Unknown constraint (%s)."
                            .formatted(constraint.getConstraintRef()));
        }
        return constraintMatchList;
    }

    private void clearMaps() {
        constraintIdToConstraintMatchTotalMap = null;
        indictmentMap = null;
    }

    public ConstraintMatchPolicy getConstraintMatchPolicy() {
        return constraintMatchPolicy;
    }

    public final Map<String, ConstraintMatchTotal<Score_>> getConstraintIdToConstraintMatchTotalMap() {
        if (!constraintMatchPolicy.isEnabled()) {
            throw new IllegalStateException("Impossible state: Method called while constraint matching is disabled.");
        } else if (constraintIdToConstraintMatchTotalMap == null) {
            rebuildConstraintMatchTotals();
        }
        return constraintIdToConstraintMatchTotalMap;
    }

    private void rebuildConstraintMatchTotals() {
        var constraintIdToConstraintMatchTotalMap = new TreeMap<String, ConstraintMatchTotal<Score_>>();
        for (var entry : constraintMatchMap.entrySet()) {
            var constraint = entry.getKey();
            var constraintMatchTotal =
                    new DefaultConstraintMatchTotal<>(constraint.getConstraintRef(), constraintWeightMap.get(constraint));
            for (var carrier : entry.getValue()) {
                // Constraint match instances are only created here when we actually need them.
                var constraintMatch = carrier.get();
                constraintMatchTotal.addConstraintMatch(constraintMatch);
            }
            constraintIdToConstraintMatchTotalMap.put(constraint.getConstraintRef().constraintName(), constraintMatchTotal);
        }
        this.constraintIdToConstraintMatchTotalMap = constraintIdToConstraintMatchTotalMap;
    }

    public final Map<Object, Indictment<Score_>> getIndictmentMap() {
        if (!constraintMatchPolicy.isJustificationEnabled()) {
            throw new IllegalStateException("Impossible state: Method called while justifications are disabled.");
        } else if (indictmentMap == null) {
            rebuildIndictments();
        }
        return indictmentMap;
    }

    private void rebuildIndictments() {
        var workingIndictmentMap = new LinkedHashMap<Object, Indictment<Score_>>();
        for (var entry : constraintMatchMap.entrySet()) {
            for (var carrier : entry.getValue()) {
                // Constraint match instances are only created here when we actually need them.
                var constraintMatch = carrier.get();
                for (var indictedObject : constraintMatch.getIndictedObjectList()) {
                    if (indictedObject == null) { // Users may have sent null, or it came from the default mapping.
                        continue;
                    }
                    var indictment = getIndictment(workingIndictmentMap, constraintMatch, indictedObject);
                    /*
                     * Optimization: In order to not have to go over the indicted object list and remove duplicates,
                     * we use a method that will silently skip duplicate constraint matches.
                     * This is harmless because the two identical indicted objects come from the same constraint match.
                     */
                    indictment.addConstraintMatchWithoutFail(constraintMatch);
                }
            }
        }
        indictmentMap = workingIndictmentMap;
    }

    private DefaultIndictment<Score_> getIndictment(Map<Object, Indictment<Score_>> indictmentMap,
            ConstraintMatch<Score_> constraintMatch, Object indictedObject) {
        // Like computeIfAbsent(), but doesn't create a capturing lambda on the hot path.
        var indictment = (DefaultIndictment<Score_>) indictmentMap.get(indictedObject);
        if (indictment == null) {
            indictment = new DefaultIndictment<>(indictedObject, constraintMatch.getScore().zero());
            indictmentMap.put(indictedObject, indictment);
        }
        return indictment;
    }

    public Set<Constraint> getConstraints() {
        return constraintWeightMap.keySet();
    }

    private static final class ConstraintMatchCarrier<Score_ extends Score<Score_>>
            implements
            Supplier<ConstraintMatch<Score_>> {

        private final Constraint constraint;
        private final ConstraintMatchSupplier<Score_> constraintMatchSupplier;
        private final ScoreImpact<Score_> scoreImpact;
        private ConstraintMatch<Score_> constraintMatch;

        private ConstraintMatchCarrier(ConstraintMatchSupplier<Score_> constraintMatchSupplier, Constraint constraint,
                ScoreImpact<Score_> scoreImpact) {
            this.constraint = constraint;
            this.constraintMatchSupplier = constraintMatchSupplier;
            this.scoreImpact = scoreImpact;
        }

        @Override
        public ConstraintMatch<Score_> get() {
            if (constraintMatch == null) {
                // Repeated requests for score explanation should not create the same constraint match over and over.
                constraintMatch = constraintMatchSupplier.apply(constraint, scoreImpact.toScore());
            }
            return constraintMatch;
        }

    }

}
