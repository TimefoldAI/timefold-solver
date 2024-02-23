package ai.timefold.solver.test.impl.score.stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.impl.score.DefaultScoreExplanation;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion;

public final class DefaultSingleConstraintAssertion<Solution_, Score_ extends Score<Score_>>
        implements SingleConstraintAssertion {

    private final AbstractConstraint<Solution_, ?, ?> constraint;
    private final ScoreDefinition<Score_> scoreDefinition;
    private final Score_ score;
    private final Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection;
    private final Collection<ConstraintJustification> justificationCollection;
    private final Collection<Indictment<Score_>> indictmentCollection;

    DefaultSingleConstraintAssertion(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            Score_ score, Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        this.constraint = (AbstractConstraint<Solution_, ?, ?>) scoreDirectorFactory.getConstraints()[0];
        this.scoreDefinition = scoreDirectorFactory.getScoreDefinition();
        this.score = requireNonNull(score);
        this.constraintMatchTotalCollection = new ArrayList<>(requireNonNull(constraintMatchTotalMap).values());
        this.indictmentCollection = new ArrayList<>(requireNonNull(indictmentMap).values());
        this.justificationCollection = this.constraintMatchTotalCollection.stream()
                .flatMap(c -> c.getConstraintMatchSet().stream())
                .map(c -> (ConstraintJustification) c.getJustification())
                .distinct()
                .toList();
    }

    @Override
    public SingleConstraintAssertion justifiesWith(String message, ConstraintJustification... justifications) {
        assertJustification(message, justifications);
        return this;
    }

    @Override
    public SingleConstraintAssertion indictsWith(String message, Object... indictments) {
        assertIndictments(message, indictments);
        return this;
    }

    @Override
    public void penalizesBy(String message, int matchWeightTotal) {
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesBy(String message, long matchWeightTotal) {
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesBy(String message, BigDecimal matchWeightTotal) {
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizes(String message, long times) {
        assertMatchCount(ScoreImpactType.PENALTY, times, message);
    }

    @Override
    public void penalizes(String message) {
        assertMatch(ScoreImpactType.PENALTY, message);
    }

    @Override
    public void rewardsWith(String message, int matchWeightTotal) {
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWith(String message, long matchWeightTotal) {
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWith(String message, BigDecimal matchWeightTotal) {
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewards(String message, long times) {
        assertMatchCount(ScoreImpactType.REWARD, times, message);
    }

    @Override
    public void rewards(String message) {
        assertMatch(ScoreImpactType.REWARD, message);
    }

    private void validateMatchWeighTotal(Number matchWeightTotal) {
        if (matchWeightTotal.doubleValue() < 0) {
            throw new IllegalArgumentException("The matchWeightTotal (" + matchWeightTotal + ") must be positive.");
        }
    }

    private void assertImpact(ScoreImpactType scoreImpactType, Number matchWeightTotal, String message) {
        BiPredicate<Number, Number> equalityPredicate =
                NumberEqualityUtil.getEqualityPredicate(scoreDefinition, matchWeightTotal);
        Pair<Number, Number> deducedImpacts = deduceImpact();
        Number impact = deducedImpacts.key();
        ScoreImpactType actualScoreImpactType = constraint.getScoreImpactType();
        if (actualScoreImpactType == ScoreImpactType.MIXED) {
            // Impact means we need to check for expected impact type and actual impact match.
            switch (scoreImpactType) {
                case REWARD:
                    Number negatedImpact = deducedImpacts.value();
                    if (equalityPredicate.test(matchWeightTotal, negatedImpact)) {
                        return;
                    }
                    break;
                case PENALTY:
                    if (equalityPredicate.test(matchWeightTotal, impact)) {
                        return;
                    }
                    break;
            }
        } else if (actualScoreImpactType == scoreImpactType && equalityPredicate.test(matchWeightTotal, impact)) {
            // Reward and positive or penalty and negative means all is OK.
            return;
        }
        String constraintId = constraint.getConstraintRef().constraintId();
        String assertionMessage = buildAssertionErrorMessage(scoreImpactType, matchWeightTotal, actualScoreImpactType,
                impact, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertJustification(String message, ConstraintJustification... justifications) {
        // Valid empty comparison
        boolean emptyJustifications = justifications == null || justifications.length == 0;
        if (emptyJustifications && justificationCollection.isEmpty()) {
            return;
        }

        // No justifications
        if (emptyJustifications && !justificationCollection.isEmpty()) {
            String assertionMessage = buildAssertionErrorMessage("Justification", constraint.getConstraintRef().constraintId(),
                    emptyList(), emptyList(), justificationCollection, message);
            throw new AssertionError(assertionMessage);
        }

        // Empty justifications
        if (justificationCollection.isEmpty()) {
            String assertionMessage = buildAssertionErrorMessage("Justification", constraint.getConstraintRef().constraintId(),
                    emptyList(), Arrays.asList(justifications), emptyList(), message);
            throw new AssertionError(assertionMessage);
        }

        List<Object> noneMatchedList = new LinkedList<>();
        List<Object> invalidMatchList = new LinkedList<>();
        for (Object justification : justifications) {
            // No match
            if (justificationCollection.stream().noneMatch(j -> justification.getClass().isAssignableFrom(j.getClass()))) {
                noneMatchedList.add(justification);
                continue;
            }
            // Test invalid match
            if (justificationCollection.stream().noneMatch(justification::equals)) {
                invalidMatchList.add(justification);
            }
        }
        if (noneMatchedList.isEmpty() && invalidMatchList.isEmpty()) {
            return;
        }
        String assertionMessage = buildAssertionErrorMessage("Justification", constraint.getConstraintRef().constraintId(),
                noneMatchedList, invalidMatchList, justificationCollection, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertIndictments(String message, Object... indictments) {
        boolean emptyIndictments = indictments == null || indictments.length == 0;
        // Valid empty comparison
        if (emptyIndictments && indictmentCollection.isEmpty()) {
            return;
        }

        // No indictments
        Collection<Object> indictmentObjectList = indictmentCollection.stream().map(Indictment::getIndictedObject).toList();
        if (emptyIndictments && !indictmentObjectList.isEmpty()) {
            String assertionMessage = buildAssertionErrorMessage("Indictment", constraint.getConstraintRef().constraintId(),
                    emptyList(), emptyList(), indictmentObjectList, message);
            throw new AssertionError(assertionMessage);
        }

        // Empty indictments
        if (indictmentObjectList.isEmpty()) {
            String assertionMessage = buildAssertionErrorMessage("Indictment", constraint.getConstraintRef().constraintId(),
                    emptyList(), Arrays.asList(indictments), emptyList(), message);
            throw new AssertionError(assertionMessage);
        }

        List<Object> noneMatchedList = new LinkedList<>();
        List<Object> invalidMatchList = new LinkedList<>();
        for (Object indictment : indictments) {
            // No match
            if (indictmentObjectList.stream().noneMatch(j -> indictment.getClass().isAssignableFrom(j.getClass()))) {
                noneMatchedList.add(indictment);
                continue;
            }
            // Test invalid match
            if (indictmentObjectList.stream().noneMatch(indictment::equals)) {
                invalidMatchList.add(indictment);
            }
        }
        if (noneMatchedList.isEmpty() && invalidMatchList.isEmpty()) {
            return;
        }
        String assertionMessage = buildAssertionErrorMessage("Indictment", constraint.getConstraintRef().constraintId(),
                noneMatchedList, invalidMatchList, indictmentObjectList, message);
        throw new AssertionError(assertionMessage);
    }

    /**
     * Returns sum total of constraint match impacts,
     * deduced from constraint matches.
     *
     * @return never null; key is the deduced impact, the value its negation
     */
    private Pair<Number, Number> deduceImpact() {
        Score_ zeroScore = scoreDefinition.getZeroScore();
        Number zero = zeroScore.toLevelNumbers()[0]; // Zero in the exact numeric type expected by the caller.
        if (constraintMatchTotalCollection.isEmpty()) {
            return new Pair<>(zero, zero);
        }
        // We do not know the matchWeight, so we need to deduce it.
        // Constraint matches give us a score, whose levels are in the form of (matchWeight * constraintWeight).
        // Here, we strip the constraintWeight.
        Score_ totalMatchWeightedScore = constraintMatchTotalCollection.stream()
                .map(matchScore -> scoreDefinition.divideBySanitizedDivisor(matchScore.getScore(),
                        matchScore.getConstraintWeight()))
                .reduce(zeroScore, Score::add);
        // Each level of the resulting score now has to be the same number, the matchWeight.
        // Except for where the number is zero.
        Number deducedImpact = retrieveImpact(totalMatchWeightedScore, zero);
        if (deducedImpact.equals(zero)) {
            return new Pair<>(zero, zero);
        }
        Number negatedDeducedImpact = retrieveImpact(totalMatchWeightedScore.negate(), zero);
        return new Pair<>(deducedImpact, negatedDeducedImpact);
    }

    private Number retrieveImpact(Score_ score, Number zero) {
        Number[] levelNumbers = score.toLevelNumbers();
        List<Number> impacts = Arrays.stream(levelNumbers)
                .distinct()
                .filter(matchWeight -> !Objects.equals(matchWeight, zero))
                .toList();
        switch (impacts.size()) {
            case 0:
                return zero;
            case 1:
                return impacts.get(0);
            default:
                throw new IllegalStateException("Impossible state: expecting at most one match weight (" +
                        impacts.size() + ") in matchWeightedScore level numbers (" + Arrays.toString(levelNumbers) + ").");
        }
    }

    private void assertMatchCount(ScoreImpactType scoreImpactType, long expectedMatchCount, String message) {
        long actualMatchCount = determineMatchCount(scoreImpactType);
        if (actualMatchCount == expectedMatchCount) {
            return;
        }
        String constraintId = constraint.getConstraintRef().constraintId();
        String assertionMessage =
                buildAssertionErrorMessage(scoreImpactType, expectedMatchCount, actualMatchCount, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertMatch(ScoreImpactType scoreImpactType, String message) {
        if (determineMatchCount(scoreImpactType) > 0) {
            return;
        }
        String constraintId = constraint.getConstraintRef().constraintId();
        String assertionMessage = buildAssertionErrorMessage(scoreImpactType, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private long determineMatchCount(ScoreImpactType scoreImpactType) {
        if (constraintMatchTotalCollection.isEmpty()) {
            return 0;
        }
        ScoreImpactType actualImpactType = constraint.getScoreImpactType();

        if (actualImpactType != scoreImpactType && actualImpactType != ScoreImpactType.MIXED) {
            return 0;
        }
        Score_ zeroScore = scoreDefinition.getZeroScore();
        return constraintMatchTotalCollection.stream()
                .mapToLong(constraintMatchTotal -> {
                    if (actualImpactType == ScoreImpactType.MIXED) {
                        boolean isImpactPositive = constraintMatchTotal.getScore().compareTo(zeroScore) > 0;
                        boolean isImpactNegative = constraintMatchTotal.getScore().compareTo(zeroScore) < 0;
                        if (isImpactPositive && scoreImpactType == ScoreImpactType.PENALTY) {
                            return constraintMatchTotal.getConstraintMatchSet().size();
                        } else if (isImpactNegative && scoreImpactType == ScoreImpactType.REWARD) {
                            return constraintMatchTotal.getConstraintMatchSet().size();
                        } else {
                            return 0;
                        }
                    } else {
                        return constraintMatchTotal.getConstraintMatchSet().size();
                    }
                })
                .sum();
    }

    private String buildAssertionErrorMessage(ScoreImpactType expectedImpactType, Number expectedImpact,
            ScoreImpactType actualImpactType, Number actualImpact, String constraintId, String message) {
        String expectation = message != null ? message : "Broken expectation.";
        String preformattedMessage = "%s%n" +
                "%18s: %s%n" +
                "%18s: %s (%s)%n" +
                "%18s: %s (%s)%n%n" +
                "  %s";
        String expectedImpactLabel = "Expected " + getImpactTypeLabel(expectedImpactType);
        String actualImpactLabel = "Actual " + getImpactTypeLabel(actualImpactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel, expectedImpact, expectedImpact.getClass(),
                actualImpactLabel, actualImpact, actualImpact.getClass(),
                DefaultScoreExplanation.explainScore(score, constraintMatchTotalCollection, indictmentCollection));
    }

    private String buildAssertionErrorMessage(ScoreImpactType impactType, long expectedTimes, long actualTimes,
            String constraintId, String message) {
        String expectation = message != null ? message : "Broken expectation.";
        String preformattedMessage = "%s%n" +
                "%18s: %s%n" +
                "%18s: %s time(s)%n" +
                "%18s: %s time(s)%n%n" +
                "  %s";
        String expectedImpactLabel = "Expected " + getImpactTypeLabel(impactType);
        String actualImpactLabel = "Actual " + getImpactTypeLabel(impactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel, expectedTimes,
                actualImpactLabel, actualTimes,
                DefaultScoreExplanation.explainScore(score, constraintMatchTotalCollection, indictmentCollection));
    }

    private String buildAssertionErrorMessage(ScoreImpactType impactType, String constraintId, String message) {
        String expectation = message != null ? message : "Broken expectation.";
        String preformattedMessage = "%s%n" +
                "%18s: %s%n" +
                "%18s but there was none.%n%n" +
                "  %s";
        String expectedImpactLabel = "Expected " + getImpactTypeLabel(impactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel,
                DefaultScoreExplanation.explainScore(score, constraintMatchTotalCollection, indictmentCollection));
    }

    private static String buildAssertionErrorMessage(String type, String constraintId, Collection<?> noneMatchedCollection,
            Collection<?> invalidMatchedCollection, Collection<?> actualCollection,
            String message) {
        String expectation = message != null ? message : "Broken expectation.";
        StringBuilder preformattedMessage = new StringBuilder("%s%n")
                .append("%18s: %s%n");
        List<Object> params = new LinkedList<>();
        params.add(expectation);
        params.add(type);
        params.add(constraintId);
        if (!noneMatchedCollection.isEmpty()) {
            preformattedMessage.append("%22s%n");
            preformattedMessage.append("%24s%n");
            params.add("No match:");
            params.add("Expected:");
            noneMatchedCollection.forEach(indictment -> {
                preformattedMessage.append("%26s%s%n");
                params.add("");
                params.add(indictment);
            });
            preformattedMessage.append("%24s%n");
            params.add("Actual:");
            actualCollection.forEach(actual -> {
                preformattedMessage.append("%26s%s%n");
                params.add("");
                params.add(actual);
            });
        }
        if (!invalidMatchedCollection.isEmpty() || (noneMatchedCollection.isEmpty() && !actualCollection.isEmpty())) {
            preformattedMessage.append("%22s%n");
            preformattedMessage.append("%24s%n");
            params.add("Invalid match:");
            params.add("Expected:");
            if (invalidMatchedCollection.isEmpty()) {
                preformattedMessage.append("%26s%s%n");
                params.add("");
                params.add("No " + type);
            } else {
                invalidMatchedCollection.forEach(indictment -> {
                    preformattedMessage.append("%26s%s%n");
                    params.add("");
                    params.add(indictment);
                });
            }
            preformattedMessage.append("%24s%n");
            params.add("Actual:");
            if (actualCollection.isEmpty()) {
                preformattedMessage.append("%26s%s%n");
                params.add("");
                params.add("No " + type);
            } else {
                actualCollection.forEach(actual -> {
                    preformattedMessage.append("%26s%s%n");
                    params.add("");
                    params.add(actual);
                });
            }
        }
        return String.format(preformattedMessage.toString(), params.toArray());
    }

    private String getImpactTypeLabel(ScoreImpactType scoreImpactType) {
        if (scoreImpactType == ScoreImpactType.PENALTY) {
            return "penalty";
        } else if (scoreImpactType == ScoreImpactType.REWARD) {
            return "reward";
        } else { // Needs to work with null.
            return "impact";
        }
    }

}
