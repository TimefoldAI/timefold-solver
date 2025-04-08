package ai.timefold.solver.test.impl.score.stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.impl.score.DefaultScoreExplanation;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract sealed class AbstractSingleConstraintAssertion<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraintAssertion<Solution_, Score_>
        implements SingleConstraintAssertion
        permits DefaultSingleConstraintAssertion, DefaultShadowVariableAwareSingleConstraintAssertion {

    private final AbstractConstraint<Solution_, ?, ?> constraint;
    private final ScoreDefinition<Score_> scoreDefinition;
    private InnerScore<Score_> actualScore;
    private Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection;
    private Collection<ConstraintJustification> justificationCollection;
    private Collection<Indictment<Score_>> indictmentCollection;

    @SuppressWarnings("unchecked")
    AbstractSingleConstraintAssertion(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory) {
        super(scoreDirectorFactory);
        this.constraint = (AbstractConstraint<Solution_, ?, ?>) scoreDirectorFactory.getConstraintMetaModel()
                .getConstraints()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Impossible state: no constraint found."));
        this.scoreDefinition = scoreDirectorFactory.getScoreDefinition();
    }

    @Override
    final void update(InnerScore<Score_> innerScore, Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        this.actualScore = InnerScore.fullyAssigned(requireNonNull(innerScore).raw()); // Strip initialization information.
        this.constraintMatchTotalCollection = new ArrayList<>(requireNonNull(constraintMatchTotalMap).values());
        this.indictmentCollection = new ArrayList<>(requireNonNull(indictmentMap).values());
        this.justificationCollection = this.constraintMatchTotalCollection.stream()
                .flatMap(c -> c.getConstraintMatchSet().stream())
                .map(c -> (ConstraintJustification) c.getJustification())
                .distinct()
                .toList();
        toggleInitialized();
    }

    @Override
    public @NonNull SingleConstraintAssertion justifiesWith(String message,
            @NonNull ConstraintJustification @NonNull... justifications) {
        ensureInitialized();
        assertJustification(message, false, justifications);
        return this;
    }

    @Override
    public @NonNull SingleConstraintAssertion indictsWith(@Nullable String message, @NonNull Object @NonNull... indictments) {
        ensureInitialized();
        assertIndictments(message, false, indictments);
        return this;
    }

    @Override
    public @NonNull SingleConstraintAssertion justifiesWithExactly(@Nullable String message,
            @NonNull ConstraintJustification @NonNull... justifications) {
        ensureInitialized();
        assertJustification(message, true, justifications);
        return this;
    }

    @Override
    public @NonNull SingleConstraintAssertion indictsWithExactly(@Nullable String message,
            @NonNull Object @NonNull... indictments) {
        ensureInitialized();
        assertIndictments(message, true, indictments);
        return this;
    }

    @Override
    public void penalizesBy(@Nullable String message, int matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesBy(@Nullable String message, long matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesBy(@Nullable String message, @NonNull BigDecimal matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizes(@Nullable String message, long times) {
        ensureInitialized();
        assertMatchCount(ScoreImpactType.PENALTY, times, message);
    }

    @Override
    public void penalizes(@Nullable String message) {
        ensureInitialized();
        assertMatch(ScoreImpactType.PENALTY, message);
    }

    @Override
    public void rewardsWith(@Nullable String message, int matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWith(@Nullable String message, long matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWith(@Nullable String message, @NonNull BigDecimal matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    private static void validateMatchWeighTotal(Number matchWeightTotal) {
        if (matchWeightTotal.doubleValue() < 0) {
            throw new IllegalArgumentException("The matchWeightTotal (%s) must be positive.".formatted(matchWeightTotal));
        }
    }

    @Override
    public void rewards(@Nullable String message, long times) {
        ensureInitialized();
        assertMatchCount(ScoreImpactType.REWARD, times, message);
    }

    @Override
    public void rewards(String message) {
        ensureInitialized();
        assertMatch(ScoreImpactType.REWARD, message);
    }

    @Override
    public void penalizesByMoreThan(@Nullable String message, int matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertMoreThanImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesByMoreThan(String message, long matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertMoreThanImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesByMoreThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertMoreThanImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesMoreThan(@Nullable String message, long times) {
        ensureInitialized();
        assertMoreThanMatchCount(ScoreImpactType.PENALTY, times, message);
    }

    @Override
    public void rewardsWithMoreThan(@Nullable String message, int matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertMoreThanImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWithMoreThan(@Nullable String message, long matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertMoreThanImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWithMoreThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal) {
        ensureInitialized();
        validateMatchWeighTotal(matchWeightTotal);
        assertMoreThanImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsMoreThan(@Nullable String message, long times) {
        ensureInitialized();
        assertMoreThanMatchCount(ScoreImpactType.REWARD, times, message);
    }

    @Override
    public void penalizesByLessThan(@Nullable String message, int matchWeightTotal) {
        ensureInitialized();
        validateLessThanMatchWeighTotal(matchWeightTotal);
        assertLessThanImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesByLessThan(@Nullable String message, long matchWeightTotal) {
        ensureInitialized();
        validateLessThanMatchWeighTotal(matchWeightTotal);
        assertLessThanImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesByLessThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal) {
        ensureInitialized();
        validateLessThanMatchWeighTotal(matchWeightTotal);
        assertLessThanImpact(ScoreImpactType.PENALTY, matchWeightTotal, message);
    }

    @Override
    public void penalizesLessThan(@Nullable String message, long times) {
        ensureInitialized();
        validateLessThanMatchCount(times);
        assertLessThanMatchCount(ScoreImpactType.PENALTY, times, message);
    }

    @Override
    public void rewardsWithLessThan(@Nullable String message, int matchWeightTotal) {
        ensureInitialized();
        validateLessThanMatchWeighTotal(matchWeightTotal);
        assertLessThanImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWithLessThan(String message, long matchWeightTotal) {
        ensureInitialized();
        validateLessThanMatchWeighTotal(matchWeightTotal);
        assertLessThanImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    @Override
    public void rewardsWithLessThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal) {
        ensureInitialized();
        validateLessThanMatchWeighTotal(matchWeightTotal);
        assertLessThanImpact(ScoreImpactType.REWARD, matchWeightTotal, message);
    }

    private static void validateLessThanMatchWeighTotal(Number matchWeightTotal) {
        if (matchWeightTotal.doubleValue() < 1) {
            throw new IllegalArgumentException("The matchWeightTotal (%s) must be greater than 0.".formatted(matchWeightTotal));
        }
    }

    @Override
    public void rewardsLessThan(String message, long times) {
        ensureInitialized();
        validateLessThanMatchCount(times);
        assertLessThanMatchCount(ScoreImpactType.REWARD, times, message);
    }

    private static void validateLessThanMatchCount(Number matchCount) {
        if (matchCount.doubleValue() < 1) {
            throw new IllegalArgumentException("The match count (%s) must be greater than 0.".formatted(matchCount));
        }
    }

    private void assertImpact(ScoreImpactType scoreImpactType, Number matchWeightTotal, String message) {
        var equalityPredicate = NumberEqualityUtil.getEqualityPredicate(scoreDefinition, matchWeightTotal);
        var deducedImpacts = deduceImpact();
        var impact = deducedImpacts.key();
        var actualScoreImpactType = constraint.getScoreImpactType();
        if (actualScoreImpactType == ScoreImpactType.MIXED) {
            // Impact means we need to check for expected impact type and actual impact match.
            if (requireNonNull(scoreImpactType) == ScoreImpactType.REWARD) {
                var negatedImpact = deducedImpacts.value();
                if (equalityPredicate.test(matchWeightTotal, negatedImpact)) {
                    return;
                }
            } else if (scoreImpactType == ScoreImpactType.PENALTY && equalityPredicate.test(matchWeightTotal, impact)) {
                return;
            }
        } else if (actualScoreImpactType == scoreImpactType && equalityPredicate.test(matchWeightTotal, impact)) {
            // Reward and positive or penalty and negative means all is OK.
            return;
        }
        var constraintId = constraint.getConstraintRef().constraintId();
        var assertionMessage = buildAssertionErrorMessage(scoreImpactType, matchWeightTotal, actualScoreImpactType,
                impact, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertMoreThanImpact(ScoreImpactType scoreImpactType, Number matchWeightTotal, String message) {
        var comparator = NumberEqualityUtil.getComparison(matchWeightTotal);
        var deducedImpacts = deduceImpact();
        var impact = deducedImpacts.key();
        var actualScoreImpactType = constraint.getScoreImpactType();
        if (actualScoreImpactType == ScoreImpactType.MIXED) {
            // Impact means we need to check for expected impact type and actual impact match.
            if (requireNonNull(scoreImpactType) == ScoreImpactType.REWARD) {
                var negatedImpact = deducedImpacts.value();
                if (comparator.compare(matchWeightTotal, negatedImpact) < 0) {
                    return;
                }
            } else if (scoreImpactType == ScoreImpactType.PENALTY && comparator.compare(matchWeightTotal, impact) < 0) {
                return;
            }
        } else if (actualScoreImpactType == scoreImpactType && comparator.compare(matchWeightTotal, impact) < 0) {
            // Reward and positive or penalty and negative means all is OK.
            return;
        }
        var constraintId = constraint.getConstraintRef().constraintId();
        var assertionMessage = buildMoreThanAssertionErrorMessage(scoreImpactType, matchWeightTotal, actualScoreImpactType,
                impact, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertLessThanImpact(ScoreImpactType scoreImpactType, Number matchWeightTotal, String message) {
        var comparator = NumberEqualityUtil.getComparison(matchWeightTotal);
        var deducedImpacts = deduceImpact();
        var impact = deducedImpacts.key();
        var actualScoreImpactType = constraint.getScoreImpactType();
        if (actualScoreImpactType == ScoreImpactType.MIXED) {
            // Impact means we need to check for expected impact type and actual impact match.
            if (requireNonNull(scoreImpactType) == ScoreImpactType.REWARD) {
                var negatedImpact = deducedImpacts.value();
                if (comparator.compare(matchWeightTotal, negatedImpact) > 0) {
                    return;
                }
            } else if (scoreImpactType == ScoreImpactType.PENALTY && comparator.compare(matchWeightTotal, impact) > 0) {
                return;
            }
        } else if (actualScoreImpactType == scoreImpactType && comparator.compare(matchWeightTotal, impact) > 0) {
            // Reward and positive or penalty and negative means all is OK.
            return;
        }
        var constraintId = constraint.getConstraintRef().constraintId();
        var assertionMessage = buildLessThanAssertionErrorMessage(scoreImpactType, matchWeightTotal, actualScoreImpactType,
                impact, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertJustification(String message, boolean completeValidation, ConstraintJustification... justifications) {
        // Valid empty comparison
        var emptyJustifications = justifications == null || justifications.length == 0;
        if (emptyJustifications && justificationCollection.isEmpty()) {
            return;
        }

        // No justifications
        if (emptyJustifications) {
            var assertionMessage = buildAssertionErrorMessage("Justification", constraint.getConstraintRef().constraintId(),
                    justificationCollection, emptyList(), emptyList(), justificationCollection, message);
            throw new AssertionError(assertionMessage);
        }

        // Empty justifications
        if (justificationCollection.isEmpty()) {
            var assertionMessage = buildAssertionErrorMessage("Justification", constraint.getConstraintRef().constraintId(),
                    emptyList(), Arrays.asList(justifications), Arrays.asList(justifications), emptyList(), message);
            throw new AssertionError(assertionMessage);
        }

        var expectedNotFound = new ArrayList<>(justificationCollection.size());
        for (var justification : justifications) {
            // Test invalid match
            if (justificationCollection.stream().noneMatch(justification::equals)) {
                expectedNotFound.add(justification);
            }
        }
        List<ConstraintJustification> unexpectedFound = emptyList();
        if (completeValidation) {
            unexpectedFound = justificationCollection.stream()
                    .filter(justification -> Stream.of(justifications).noneMatch(justification::equals))
                    .toList();
        }
        if (expectedNotFound.isEmpty() && unexpectedFound.isEmpty()) {
            return;
        }
        var assertionMessage = buildAssertionErrorMessage("Justification", constraint.getConstraintRef().constraintId(),
                unexpectedFound, expectedNotFound, Arrays.asList(justifications), justificationCollection, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertIndictments(String message, boolean completeValidation, Object... indictments) {
        var emptyIndictments = indictments == null || indictments.length == 0;
        // Valid empty comparison
        if (emptyIndictments && indictmentCollection.isEmpty()) {
            return;
        }

        // No indictments
        var indictmentObjectList = indictmentCollection.stream().map(Indictment::getIndictedObject).toList();
        if (emptyIndictments && !indictmentObjectList.isEmpty()) {
            var assertionMessage = buildAssertionErrorMessage("Indictment", constraint.getConstraintRef().constraintId(),
                    indictmentObjectList, emptyList(), emptyList(), indictmentObjectList, message);
            throw new AssertionError(assertionMessage);
        }

        // Empty indictments
        if (indictmentObjectList.isEmpty()) {
            var assertionMessage = buildAssertionErrorMessage("Indictment", constraint.getConstraintRef().constraintId(),
                    emptyList(), Arrays.asList(indictments), Arrays.asList(indictments), emptyList(), message);
            throw new AssertionError(assertionMessage);
        }

        var expectedNotFound = new ArrayList<>(indictmentObjectList.size());
        for (var indictment : indictments) {
            // Test invalid match
            if (indictmentObjectList.stream().noneMatch(indictment::equals)) {
                expectedNotFound.add(indictment);
            }
        }
        var unexpectedFound = emptyList();
        if (completeValidation) {
            unexpectedFound = indictmentObjectList.stream()
                    .filter(indictment -> Arrays.stream(indictments).noneMatch(indictment::equals))
                    .toList();
        }
        if (expectedNotFound.isEmpty() && unexpectedFound.isEmpty()) {
            return;
        }
        var assertionMessage = buildAssertionErrorMessage("Indictment", constraint.getConstraintRef().constraintId(),
                unexpectedFound, expectedNotFound, Arrays.asList(indictments), indictmentObjectList, message);
        throw new AssertionError(assertionMessage);
    }

    /**
     * Returns sum total of constraint match impacts,
     * deduced from constraint matches.
     *
     * @return never null; key is the deduced impact, the value its negation
     */
    private Pair<Number, Number> deduceImpact() {
        var zeroScore = scoreDefinition.getZeroScore();
        var zero = zeroScore.toLevelNumbers()[0]; // Zero in the exact numeric type expected by the caller.
        if (constraintMatchTotalCollection.isEmpty()) {
            return new Pair<>(zero, zero);
        }
        // We do not know the matchWeight, so we need to deduce it.
        // Constraint matches give us a score, whose levels are in the form of (matchWeight * constraintWeight).
        // Here, we strip the constraintWeight.
        var totalMatchWeightedScore = constraintMatchTotalCollection.stream()
                .map(matchScore -> scoreDefinition.divideBySanitizedDivisor(matchScore.getScore(),
                        matchScore.getConstraintWeight()))
                .reduce(zeroScore, Score::add);
        // Each level of the resulting score now has to be the same number, the matchWeight.
        // Except for where the number is zero.
        var deducedImpact = retrieveImpact(totalMatchWeightedScore, zero);
        if (deducedImpact.equals(zero)) {
            return new Pair<>(zero, zero);
        }
        var negatedDeducedImpact = retrieveImpact(totalMatchWeightedScore.negate(), zero);
        return new Pair<>(deducedImpact, negatedDeducedImpact);
    }

    private Number retrieveImpact(Score_ score, Number zero) {
        var levelNumbers = score.toLevelNumbers();
        var impacts = Arrays.stream(levelNumbers)
                .distinct()
                .filter(matchWeight -> !Objects.equals(matchWeight, zero))
                .toList();
        return switch (impacts.size()) {
            case 0 -> zero;
            case 1 -> impacts.get(0);
            default -> throw new IllegalStateException(
                    "Impossible state: expecting at most one match weight (%d) in matchWeightedScore level numbers (%s)."
                            .formatted(impacts.size(), Arrays.toString(levelNumbers)));
        };
    }

    private void assertMatchCount(ScoreImpactType scoreImpactType, long expectedMatchCount, String message) {
        var actualMatchCount = determineMatchCount(scoreImpactType);
        if (actualMatchCount == expectedMatchCount) {
            return;
        }
        var constraintId = constraint.getConstraintRef().constraintId();
        var assertionMessage =
                buildAssertionErrorMessage(scoreImpactType, expectedMatchCount, actualMatchCount, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertMoreThanMatchCount(ScoreImpactType scoreImpactType, long expectedMatchCount, String message) {
        var actualMatchCount = determineMatchCount(scoreImpactType);
        if (actualMatchCount > expectedMatchCount) {
            return;
        }
        var constraintId = constraint.getConstraintRef().constraintId();
        var assertionMessage = buildMoreThanAssertionErrorMessage(scoreImpactType, expectedMatchCount, actualMatchCount,
                constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertLessThanMatchCount(ScoreImpactType scoreImpactType, long expectedMatchCount, String message) {
        var actualMatchCount = determineMatchCount(scoreImpactType);
        if (actualMatchCount < expectedMatchCount) {
            return;
        }
        var constraintId = constraint.getConstraintRef().constraintId();
        var assertionMessage = buildLessThanAssertionErrorMessage(scoreImpactType, expectedMatchCount, actualMatchCount,
                constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private void assertMatch(ScoreImpactType scoreImpactType, String message) {
        if (determineMatchCount(scoreImpactType) > 0) {
            return;
        }
        var constraintId = constraint.getConstraintRef().constraintId();
        var assertionMessage = buildAssertionErrorMessage(scoreImpactType, constraintId, message);
        throw new AssertionError(assertionMessage);
    }

    private long determineMatchCount(ScoreImpactType scoreImpactType) {
        if (constraintMatchTotalCollection.isEmpty()) {
            return 0;
        }
        var actualImpactType = constraint.getScoreImpactType();

        if (actualImpactType != scoreImpactType && actualImpactType != ScoreImpactType.MIXED) {
            return 0;
        }
        var zeroScore = scoreDefinition.getZeroScore();
        return constraintMatchTotalCollection.stream()
                .mapToLong(constraintMatchTotal -> {
                    if (actualImpactType == ScoreImpactType.MIXED) {
                        var isImpactPositive = constraintMatchTotal.getScore().compareTo(zeroScore) > 0;
                        var isImpactNegative = constraintMatchTotal.getScore().compareTo(zeroScore) < 0;
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
        var expectation = message != null ? message : "Broken expectation.";
        var preformattedMessage = "%s%n%18s: %s%n%18s: %s (%s)%n%18s: %s (%s)%n%n  %s";
        var expectedImpactLabel = "Expected " + getImpactTypeLabel(expectedImpactType);
        var actualImpactLabel = "Actual " + getImpactTypeLabel(actualImpactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel, expectedImpact, expectedImpact.getClass(),
                actualImpactLabel, actualImpact, actualImpact.getClass(),
                DefaultScoreExplanation.explainScore(actualScore, constraintMatchTotalCollection, indictmentCollection));
    }

    private String buildMoreThanAssertionErrorMessage(ScoreImpactType expectedImpactType, Number expectedImpact,
            ScoreImpactType actualImpactType, Number actualImpact, String constraintId, String message) {
        return buildMoreOrLessThanAssertionErrorMessage(expectedImpactType, "more than", expectedImpact, actualImpactType,
                actualImpact, constraintId, message);
    }

    private String buildLessThanAssertionErrorMessage(ScoreImpactType expectedImpactType, Number expectedImpact,
            ScoreImpactType actualImpactType, Number actualImpact, String constraintId, String message) {
        return buildMoreOrLessThanAssertionErrorMessage(expectedImpactType, "less than", expectedImpact, actualImpactType,
                actualImpact, constraintId, message);
    }

    private String buildMoreOrLessThanAssertionErrorMessage(ScoreImpactType expectedImpactType, String moreOrLessThan,
            Number expectedImpact, ScoreImpactType actualImpactType, Number actualImpact, String constraintId, String message) {
        var expectation = message != null ? message : "Broken expectation.";
        var preformattedMessage = "%s%n%28s: %s%n%28s: %s (%s)%n%28s: %s (%s)%n%n  %s";
        var expectedImpactLabel = "Expected " + getImpactTypeLabel(expectedImpactType) + " " + moreOrLessThan;
        var actualImpactLabel = "Actual " + getImpactTypeLabel(actualImpactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel, expectedImpact, expectedImpact.getClass(),
                actualImpactLabel, actualImpact, actualImpact.getClass(),
                DefaultScoreExplanation.explainScore(actualScore, constraintMatchTotalCollection, indictmentCollection));
    }

    private String buildAssertionErrorMessage(ScoreImpactType impactType, long expectedTimes, long actualTimes,
            String constraintId, String message) {
        var expectation = message != null ? message : "Broken expectation.";
        var preformattedMessage = "%s%n%18s: %s%n%18s: %s time(s)%n%18s: %s time(s)%n%n  %s";
        var expectedImpactLabel = "Expected " + getImpactTypeLabel(impactType);
        var actualImpactLabel = "Actual " + getImpactTypeLabel(impactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel, expectedTimes,
                actualImpactLabel, actualTimes,
                DefaultScoreExplanation.explainScore(actualScore, constraintMatchTotalCollection, indictmentCollection));
    }

    private String buildMoreThanAssertionErrorMessage(ScoreImpactType impactType, long expectedTimes, long actualTimes,
            String constraintId, String message) {
        return buildMoreOrLessThanAssertionErrorMessage(impactType, "more than", expectedTimes, actualTimes, constraintId,
                message);
    }

    private String buildLessThanAssertionErrorMessage(ScoreImpactType impactType, long expectedTimes, long actualTimes,
            String constraintId, String message) {
        return buildMoreOrLessThanAssertionErrorMessage(impactType, "less than", expectedTimes, actualTimes, constraintId,
                message);
    }

    private String buildMoreOrLessThanAssertionErrorMessage(ScoreImpactType impactType, String moreOrLessThan,
            long expectedTimes, long actualTimes,
            String constraintId, String message) {
        var expectation = message != null ? message : "Broken expectation.";
        var preformattedMessage = "%s%n%28s: %s%n%28s: %s time(s)%n%28s: %s time(s)%n%n  %s";
        var expectedImpactLabel = "Expected " + getImpactTypeLabel(impactType) + " " + moreOrLessThan;
        var actualImpactLabel = "Actual " + getImpactTypeLabel(impactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel, expectedTimes,
                actualImpactLabel, actualTimes,
                DefaultScoreExplanation.explainScore(actualScore, constraintMatchTotalCollection, indictmentCollection));
    }

    private String buildAssertionErrorMessage(ScoreImpactType impactType, String constraintId, String message) {
        var expectation = message != null ? message : "Broken expectation.";
        var preformattedMessage = "%s%n%18s: %s%n%18s but there was none.%n%n  %s";
        var expectedImpactLabel = "Expected " + getImpactTypeLabel(impactType);
        return String.format(preformattedMessage,
                expectation,
                "Constraint", constraintId,
                expectedImpactLabel,
                DefaultScoreExplanation.explainScore(actualScore, constraintMatchTotalCollection, indictmentCollection));
    }

    private static String buildAssertionErrorMessage(String type, String constraintId, Collection<?> unexpectedFound,
            Collection<?> expectedNotFound, Collection<?> expectedCollection, Collection<?> actualCollection,
            String message) {
        var expectation = message != null ? message : "Broken expectation.";
        var preformattedMessage = new StringBuilder("%s%n")
                .append("%18s: %s%n");
        var params = new ArrayList<>();
        params.add(expectation);
        params.add(type);
        params.add(constraintId);
        preformattedMessage.append("%24s%n");
        params.add("Expected:");
        if (expectedCollection.isEmpty()) {
            preformattedMessage.append("%26s%s%n");
            params.add("");
            params.add("No " + type);
        } else {
            expectedCollection.forEach(actual -> {
                preformattedMessage.append("%26s%s%n");
                params.add("");
                params.add(actual);
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
        if (!expectedNotFound.isEmpty()) {
            preformattedMessage.append("%24s%n");
            params.add("Expected but not found:");
            expectedNotFound.forEach(indictment -> {
                preformattedMessage.append("%26s%s%n");
                params.add("");
                params.add(indictment);
            });
        }
        if (!unexpectedFound.isEmpty()) {
            preformattedMessage.append("%24s%n");
            params.add("Unexpected but found:");
            unexpectedFound.forEach(indictment -> {
                preformattedMessage.append("%26s%s%n");
                params.add("");
                params.add(indictment);
            });
        }
        return String.format(preformattedMessage.toString(), params.toArray());
    }

    private static String getImpactTypeLabel(ScoreImpactType scoreImpactType) {
        if (scoreImpactType == ScoreImpactType.PENALTY) {
            return "penalty";
        } else if (scoreImpactType == ScoreImpactType.REWARD) {
            return "reward";
        } else { // Needs to work with null.
            return "impact";
        }
    }

}
