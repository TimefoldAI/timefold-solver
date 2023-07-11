package ai.timefold.solver.constraint.streams.common.inliner;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
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
import ai.timefold.solver.core.impl.util.CollectionUtils;

public abstract class AbstractScoreInliner<Score_ extends Score<Score_>> {

    @Deprecated(forRemoval = true)
    private static final String CUSTOM_SCORE_INLINER_CLASS_PROPERTY_NAME =
            "ai.timefold.solver.score.stream.inliner";

    public static <Score_ extends Score<Score_>, ScoreInliner_ extends AbstractScoreInliner<Score_>> ScoreInliner_
            buildScoreInliner(ScoreDefinition<Score_> scoreDefinition, int constraintCount, boolean constraintMatchEnabled) {
        if (scoreDefinition instanceof SimpleScoreDefinition) {
            return (ScoreInliner_) new SimpleScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof SimpleLongScoreDefinition) {
            return (ScoreInliner_) new SimpleLongScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof SimpleBigDecimalScoreDefinition) {
            return (ScoreInliner_) new SimpleBigDecimalScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardSoftScoreDefinition) {
            return (ScoreInliner_) new HardSoftScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardSoftLongScoreDefinition) {
            return (ScoreInliner_) new HardSoftLongScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardSoftBigDecimalScoreDefinition) {
            return (ScoreInliner_) new HardSoftBigDecimalScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardMediumSoftScoreDefinition) {
            return (ScoreInliner_) new HardMediumSoftScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardMediumSoftLongScoreDefinition) {
            return (ScoreInliner_) new HardMediumSoftLongScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof HardMediumSoftBigDecimalScoreDefinition) {
            return (ScoreInliner_) new HardMediumSoftBigDecimalScoreInliner(constraintCount, constraintMatchEnabled);
        } else if (scoreDefinition instanceof BendableScoreDefinition) {
            BendableScoreDefinition bendableScoreDefinition = (BendableScoreDefinition) scoreDefinition;
            return (ScoreInliner_) new BendableScoreInliner(constraintCount, constraintMatchEnabled,
                    bendableScoreDefinition.getHardLevelsSize(),
                    bendableScoreDefinition.getSoftLevelsSize());
        } else if (scoreDefinition instanceof BendableLongScoreDefinition) {
            BendableLongScoreDefinition bendableScoreDefinition = (BendableLongScoreDefinition) scoreDefinition;
            return (ScoreInliner_) new BendableLongScoreInliner(constraintCount, constraintMatchEnabled,
                    bendableScoreDefinition.getHardLevelsSize(),
                    bendableScoreDefinition.getSoftLevelsSize());
        } else if (scoreDefinition instanceof BendableBigDecimalScoreDefinition) {
            BendableBigDecimalScoreDefinition bendableScoreDefinition = (BendableBigDecimalScoreDefinition) scoreDefinition;
            return (ScoreInliner_) new BendableBigDecimalScoreInliner(constraintCount, constraintMatchEnabled,
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
    private final Map<String, DefaultConstraintMatchTotal<Score_>> constraintMatchTotalMap;
    private final Map<Object, DefaultIndictment<Score_>> indictmentMap;

    protected AbstractScoreInliner(int constraintCount, boolean constraintMatchEnabled) {
        this.constraintMatchEnabled = constraintMatchEnabled;
        this.constraintMatchTotalMap = constraintMatchEnabled ? CollectionUtils.newLinkedHashMap(constraintCount) : null;
        this.indictmentMap = constraintMatchEnabled ? new LinkedHashMap<>() : null;
    }

    public abstract Score_ extractScore(int initScore);

    /**
     * Create a new instance of {@link WeightedScoreImpacter} for a particular constraint.
     *
     * @param constraint never null
     * @param constraintWeight never null
     * @return never null
     */
    public abstract WeightedScoreImpacter<?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint,
            Score_ constraintWeight);

    protected final Runnable addConstraintMatch(Constraint constraint, Score_ constraintWeight, Score_ score,
            JustificationsSupplier justificationsSupplier) {
        var constraintMatchTotal = getConstraintMatchTotal(constraint, constraintWeight);
        Collection<Object> indictedObjectCollection = justificationsSupplier.indictedObjectCollection();
        var constraintMatch = constraintMatchTotal.addConstraintMatch(
                justificationsSupplier.createConstraintJustification(score),
                indictedObjectCollection, score);
        // Optimization: if the indicted objects are distinct, we can skip part of the conversion to distinct list.
        var indictedObjectList = indictedObjectCollection instanceof Set<Object> set ? CollectionUtils.toDistinctList(set)
                : CollectionUtils.toDistinctList(constraintMatch.getIndictedObjectList());
        if (indictedObjectList.isEmpty()) {
            return () -> removeConstraintMatchFromTotal(constraintMatchTotal, constraintMatch);
        } else {
            var indictmentList = new ArrayList<DefaultIndictment<Score_>>(indictedObjectList.size());
            for (var indictedObject : indictedObjectList) {
                var indictment = getIndictment(constraintMatch, indictedObject);
                indictment.addConstraintMatch(constraintMatch);
                indictmentList.add(indictment);
            }
            return () -> {
                removeConstraintMatchFromTotal(constraintMatchTotal, constraintMatch);
                removeConstraintMatchFromIndictment(indictmentList, constraintMatch);
            };
        }
    }

    private DefaultConstraintMatchTotal<Score_> getConstraintMatchTotal(Constraint constraint, Score_ constraintWeight) {
        // Like computeIfAbsent(), but doesn't create a capturing lambda on the hot path.
        String constraintId = constraint.getConstraintId();
        var constraintMatchTotal = constraintMatchTotalMap.get(constraintId);
        if (constraintMatchTotal == null) {
            constraintMatchTotal = new DefaultConstraintMatchTotal<>(constraint, constraintWeight);
            constraintMatchTotalMap.put(constraintId, constraintMatchTotal);
        }
        return constraintMatchTotal;
    }

    private DefaultIndictment<Score_> getIndictment(ConstraintMatch<Score_> constraintMatch, Object indictedObject) {
        // Like computeIfAbsent(), but doesn't create a capturing lambda on the hot path.
        var indictment = indictmentMap.get(indictedObject);
        if (indictment == null) {
            indictment = new DefaultIndictment<>(indictedObject, constraintMatch.getScore().zero());
            indictmentMap.put(indictedObject, indictment);
        }
        return indictment;
    }

    private void removeConstraintMatchFromTotal(DefaultConstraintMatchTotal<Score_> constraintMatchTotal,
            ConstraintMatch<Score_> constraintMatch) {
        constraintMatchTotal.removeConstraintMatch(constraintMatch);
        if (constraintMatchTotal.getConstraintMatchSet().isEmpty()) {
            constraintMatchTotalMap.remove(constraintMatch.getConstraintId());
        }
    }

    private void removeConstraintMatchFromIndictment(List<DefaultIndictment<Score_>> indictmentList,
            ConstraintMatch<Score_> constraintMatch) {
        for (DefaultIndictment<Score_> indictment : indictmentList) {
            indictment.removeConstraintMatch(constraintMatch);
            if (indictment.getConstraintMatchSet().isEmpty()) {
                indictmentMap.remove(indictment.getIndictedObject());
            }
        }
    }

    public boolean isConstraintMatchEnabled() {
        return constraintMatchEnabled;
    }

    public final Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        // Unchecked assignment necessary as CMT and DefaultCMT incompatible in the Map generics.
        return (Map) constraintMatchTotalMap;
    }

    public final Map<Object, Indictment<Score_>> getIndictmentMap() {
        // Unchecked assignment necessary as Indictment and DefaultIndictment incompatible in the Map generics.
        return (Map) indictmentMap;
    }

    protected final void validateConstraintWeight(Constraint constraint, Score_ constraintWeight) {
        if (constraintWeight == null || constraintWeight.isZero()) {
            throw new IllegalArgumentException("Impossible state: The constraintWeight (" +
                    constraintWeight + ") cannot be zero, constraint (" + constraint +
                    ") should have been culled during session creation.");
        }
    }

}
