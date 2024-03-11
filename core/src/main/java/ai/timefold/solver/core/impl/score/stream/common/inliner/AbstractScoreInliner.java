package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.buildin.BendableBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.BendableLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardSoftBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardSoftLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.SimpleBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.SimpleLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

/**
 * Keeps track of the working score and constraint matches for a single constraint session.
 * Every time constraint weights change, a new instance needs to be created.
 *
 * @param <Score_>
 */
public abstract class AbstractScoreInliner<Score_ extends Score<Score_>> {

    @Deprecated(forRemoval = true)
    private static final String CUSTOM_SCORE_INLINER_CLASS_PROPERTY_NAME =
            "ai.timefold.solver.score.stream.inliner";

    public static <Score_ extends Score<Score_>, ScoreInliner_ extends AbstractScoreInliner<Score_>> ScoreInliner_
            buildScoreInliner(ScoreDefinition<Score_> scoreDefinition, Map<Constraint, Score_> constraintWeightMap,
                    boolean constraintMatchEnabled) {
        if (scoreDefinition instanceof SimpleScoreDefinition) {
            return (ScoreInliner_) new SimpleScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof SimpleLongScoreDefinition) {
            return (ScoreInliner_) new SimpleLongScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof SimpleBigDecimalScoreDefinition) {
            return (ScoreInliner_) new SimpleBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardSoftScoreDefinition) {
            return (ScoreInliner_) new HardSoftScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardSoftLongScoreDefinition) {
            return (ScoreInliner_) new HardSoftLongScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardSoftBigDecimalScoreDefinition) {
            return (ScoreInliner_) new HardSoftBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardMediumSoftScoreDefinition) {
            return (ScoreInliner_) new HardMediumSoftScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardMediumSoftLongScoreDefinition) {
            return (ScoreInliner_) new HardMediumSoftLongScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardMediumSoftBigDecimalScoreDefinition) {
            return (ScoreInliner_) new HardMediumSoftBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchEnabled);
        } else if (scoreDefinition instanceof BendableScoreDefinition bendableScoreDefinition) {
            return (ScoreInliner_) new BendableScoreInliner((Map) constraintWeightMap, constraintMatchEnabled,
                    bendableScoreDefinition.getHardLevelsSize(),
                    bendableScoreDefinition.getSoftLevelsSize());
        } else if (scoreDefinition instanceof BendableLongScoreDefinition bendableScoreDefinition) {
            return (ScoreInliner_) new BendableLongScoreInliner((Map) constraintWeightMap, constraintMatchEnabled,
                    bendableScoreDefinition.getHardLevelsSize(),
                    bendableScoreDefinition.getSoftLevelsSize());
        } else if (scoreDefinition instanceof BendableBigDecimalScoreDefinition bendableScoreDefinition) {
            return (ScoreInliner_) new BendableBigDecimalScoreInliner((Map) constraintWeightMap, constraintMatchEnabled,
                    bendableScoreDefinition.getHardLevelsSize(), bendableScoreDefinition.getSoftLevelsSize());
        } else {
            String customScoreInlinerClassName = System.getProperty(CUSTOM_SCORE_INLINER_CLASS_PROPERTY_NAME);
            if (customScoreInlinerClassName == null) {
                throw new UnsupportedOperationException("Unknown score definition class (" +
                        scoreDefinition.getClass().getCanonicalName() + ").\n" +
                        "If you're attempting to use a custom score, " +
                        "provide your " + AbstractScoreInliner.class.getSimpleName() + " implementation using the '" +
                        CUSTOM_SCORE_INLINER_CLASS_PROPERTY_NAME + "' system property.\n" +
                        "Note: support for custom scores will be removed in Timefold 2.0.");
            }
            try {
                Class<?> customScoreInlinerClass = Class.forName(customScoreInlinerClassName);
                if (!AbstractScoreInliner.class.isAssignableFrom(customScoreInlinerClass)) {
                    throw new IllegalStateException("Custom score inliner class (" + customScoreInlinerClassName +
                            ") does not extend " + AbstractScoreInliner.class.getCanonicalName() + ".\n" +
                            "Note: support for custom scores will be removed in Timefold 2.0.");
                }
                return ((Class<ScoreInliner_>) customScoreInlinerClass).getConstructor()
                        .newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                    | InvocationTargetException cause) {
                throw new IllegalStateException("Custom score inliner class (" + customScoreInlinerClassName +
                        ") can not be instantiated.\n" +
                        "Maybe add a no-arg public constructor?\n" +
                        "Note: support for custom scores will be removed in Timefold 2.0.", cause);
            }
        }
    }

    protected final boolean constraintMatchEnabled;
    protected final Map<Constraint, Score_> constraintWeightMap;
    private final Map<Constraint, ElementAwareList<ConstraintMatchCarrier<Score_>>> constraintMatchMap;
    private Map<String, ConstraintMatchTotal<Score_>> constraintIdToConstraintMatchTotalMap = null;
    private Map<Object, Indictment<Score_>> indictmentMap = null;

    protected AbstractScoreInliner(Map<Constraint, Score_> constraintWeightMap, boolean constraintMatchEnabled) {
        this.constraintMatchEnabled = constraintMatchEnabled;
        constraintWeightMap.forEach(this::validateConstraintWeight);
        this.constraintWeightMap = constraintWeightMap;
        this.constraintMatchMap =
                constraintMatchEnabled ? CollectionUtils.newIdentityHashMap(constraintWeightMap.size()) : null;
        if (constraintMatchEnabled) {
            for (var constraint : constraintWeightMap.keySet()) {
                // Ensure that even constraints without matches have their entry.
                constraintMatchMap.put(constraint, new ElementAwareList<>());
            }
        }
    }

    private void validateConstraintWeight(Constraint constraint, Score_ constraintWeight) {
        if (constraintWeight == null || constraintWeight.isZero()) {
            throw new IllegalArgumentException("Impossible state: The constraintWeight (" +
                    constraintWeight + ") cannot be zero, constraint (" + constraint +
                    ") should have been culled during session creation.");
        }
    }

    public abstract Score_ extractScore(int initScore);

    /**
     * Create a new instance of {@link WeightedScoreImpacter} for a particular constraint.
     *
     * @param constraint never null
     * @return never null
     */
    public abstract WeightedScoreImpacter<Score_, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint);

    protected final UndoScoreImpacter addConstraintMatch(Constraint constraint, Score_ score,
            ConstraintMatchSupplier<Score_> constraintMatchSupplier, UndoScoreImpacter undoScoreImpact) {
        ElementAwareList<ConstraintMatchCarrier<Score_>> constraintMatchList = getConstraintMatchList(constraint);
        /*
         * Creating a constraint match is a heavy operation which may yet be undone.
         * Defer creation of the constraint match until a later point.
         */
        ElementAwareListEntry<ConstraintMatchCarrier<Score_>> entry =
                constraintMatchList.add(new ConstraintMatchCarrier<>(constraintMatchSupplier, constraint, score));
        clearMaps();
        return () -> {
            undoScoreImpact.run();
            entry.remove();
            clearMaps();
        };
    }

    private ElementAwareList<ConstraintMatchCarrier<Score_>> getConstraintMatchList(Constraint constraint) {
        // Optimization: computeIfAbsent() would have created a lambda on the hot path.
        ElementAwareList<ConstraintMatchCarrier<Score_>> constraintMatchList = constraintMatchMap.get(constraint);
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

    public boolean isConstraintMatchEnabled() {
        return constraintMatchEnabled;
    }

    public final Map<String, ConstraintMatchTotal<Score_>> getConstraintIdToConstraintMatchTotalMap() {
        if (constraintIdToConstraintMatchTotalMap == null) {
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
            constraintIdToConstraintMatchTotalMap.put(constraint.getConstraintRef().constraintId(), constraintMatchTotal);
        }
        this.constraintIdToConstraintMatchTotalMap = constraintIdToConstraintMatchTotalMap;
    }

    public final Map<Object, Indictment<Score_>> getIndictmentMap() {
        if (indictmentMap == null) {
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
                    var indictment =
                            (DefaultIndictment<Score_>) getIndictment(workingIndictmentMap, constraintMatch, indictedObject);
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

    private static final class ConstraintMatchCarrier<Score_ extends Score<Score_>>
            implements
            Supplier<ConstraintMatch<Score_>> {

        private final Constraint constraint;
        private final ConstraintMatchSupplier<Score_> constraintMatchSupplier;
        private final Score_ score;
        private ConstraintMatch<Score_> constraintMatch;

        private ConstraintMatchCarrier(ConstraintMatchSupplier<Score_> constraintMatchSupplier, Constraint constraint,
                Score_ score) {
            this.constraint = constraint;
            this.constraintMatchSupplier = constraintMatchSupplier;
            this.score = score;
        }

        @Override
        public ConstraintMatch<Score_> get() {
            if (constraintMatch == null) {
                // Repeated requests for score explanation should not create the same constraint match over and over.
                constraintMatch = constraintMatchSupplier.apply(constraint, score);
            }
            return constraintMatch;
        }

    }

}
