package ai.timefold.solver.core.api.score;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.MEDIUM_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.STRUCTURAL_LABEL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on 3 levels of {@link BigDecimal} constraints: hard, medium and soft.
 * Hard constraints have priority over medium constraints.
 * Medium constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
@NullMarked
public record HardMediumSoftBigDecimalScore(long structuralScore, BigDecimal hardScore, BigDecimal mediumScore,
        BigDecimal softScore) implements Score<HardMediumSoftBigDecimalScore> {

    public static final HardMediumSoftBigDecimalScore ZERO = new HardMediumSoftBigDecimalScore(BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO);
    public static final HardMediumSoftBigDecimalScore ONE_HARD = new HardMediumSoftBigDecimalScore(BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ZERO);
    public static final HardMediumSoftBigDecimalScore ONE_MEDIUM =
            new HardMediumSoftBigDecimalScore(BigDecimal.ZERO,
                    BigDecimal.ONE, BigDecimal.ZERO);
    public static final HardMediumSoftBigDecimalScore ONE_SOFT = new HardMediumSoftBigDecimalScore(BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ONE);

    public HardMediumSoftBigDecimalScore(BigDecimal hardScore, BigDecimal mediumScore,
            BigDecimal softScore) {
        this(0L, hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftBigDecimalScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(HardMediumSoftBigDecimalScore.class, scoreString,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
        if (scoreTokens.length == 3) {
            var hardScore = ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[0]);
            var mediumScore =
                    ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[1]);
            var softScore = ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[2]);
            return of(hardScore, mediumScore, softScore);
        } else {
            var structuralScore = ScoreUtil.parseLevelAsLong(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[0]);
            var hardScore = ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[1]);
            var mediumScore =
                    ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[2]);
            var softScore = ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[3]);
            return new HardMediumSoftBigDecimalScore(structuralScore, hardScore, mediumScore, softScore);
        }
    }

    public static HardMediumSoftBigDecimalScore of(BigDecimal hardScore, BigDecimal mediumScore,
            BigDecimal softScore) {
        if (ScoreUtil.isZero(hardScore) && ScoreUtil.isZero(mediumScore) && ScoreUtil.isZero(softScore)) {
            return ZERO;
        }
        return new HardMediumSoftBigDecimalScore(hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftBigDecimalScore ofHard(BigDecimal hardScore) {
        if (ScoreUtil.isZero(hardScore)) {
            return ZERO;
        }
        return new HardMediumSoftBigDecimalScore(hardScore, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static HardMediumSoftBigDecimalScore ofMedium(BigDecimal mediumScore) {
        if (ScoreUtil.isZero(mediumScore)) {
            return ZERO;
        }
        return new HardMediumSoftBigDecimalScore(BigDecimal.ZERO, mediumScore, BigDecimal.ZERO);
    }

    public static HardMediumSoftBigDecimalScore ofSoft(BigDecimal softScore) {
        if (ScoreUtil.isZero(softScore)) {
            return ZERO;
        }
        return new HardMediumSoftBigDecimalScore(BigDecimal.ZERO, BigDecimal.ZERO, softScore);
    }

    /**
     * A {@link PlanningSolution} is feasible if it has no broken hard constraints.
     *
     * @return true if the {@link #hardScore()} is 0 or higher
     */
    @Override
    public boolean isFeasible() {
        return structuralScore >= 0 && !ScoreUtil.isNegative(hardScore);
    }

    @Override
    public HardMediumSoftBigDecimalScore add(HardMediumSoftBigDecimalScore addend) {
        return of(hardScore.add(addend.hardScore()),
                mediumScore.add(addend.mediumScore()),
                softScore.add(addend.softScore()));
    }

    @Override
    public HardMediumSoftBigDecimalScore subtract(HardMediumSoftBigDecimalScore subtrahend) {
        return of(hardScore.subtract(subtrahend.hardScore()),
                mediumScore.subtract(subtrahend.mediumScore()),
                softScore.subtract(subtrahend.softScore()));
    }

    @Override
    public HardMediumSoftBigDecimalScore multiply(double multiplicand) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        var multiplicandBigDecimal = BigDecimal.valueOf(multiplicand);
        // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
        return of(hardScore.multiply(multiplicandBigDecimal).setScale(hardScore.scale(), RoundingMode.FLOOR),
                mediumScore.multiply(multiplicandBigDecimal).setScale(mediumScore.scale(), RoundingMode.FLOOR),
                softScore.multiply(multiplicandBigDecimal).setScale(softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public HardMediumSoftBigDecimalScore divide(double divisor) {
        var divisorBigDecimal = BigDecimal.valueOf(divisor);
        // The (unspecified) scale/precision of the divisor should have no impact on the returned scale/precision
        return of(hardScore.divide(divisorBigDecimal, hardScore.scale(), RoundingMode.FLOOR),
                mediumScore.divide(divisorBigDecimal, mediumScore.scale(), RoundingMode.FLOOR),
                softScore.divide(divisorBigDecimal, softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public HardMediumSoftBigDecimalScore power(double exponent) {
        var exponentBigDecimal = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        //  None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        return of(hardScore.pow(exponentBigDecimal.intValue()).setScale(hardScore.scale(), RoundingMode.FLOOR),
                mediumScore.pow(exponentBigDecimal.intValue()).setScale(mediumScore.scale(), RoundingMode.FLOOR),
                softScore.pow(exponentBigDecimal.intValue()).setScale(softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public HardMediumSoftBigDecimalScore abs() {
        return of(hardScore.abs(), mediumScore.abs(), softScore.abs());
    }

    @Override
    public HardMediumSoftBigDecimalScore zero() {
        return HardMediumSoftBigDecimalScore.ZERO;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { hardScore, mediumScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardMediumSoftBigDecimalScore(var otherStructuralScore, var otherHardScore, var otherMediumScore, var otherSoftScore)) {
            return structuralScore == otherStructuralScore
                    && ScoreUtil.equalsIgnoringScale(hardScore, otherHardScore)
                    && ScoreUtil.equalsIgnoringScale(mediumScore, otherMediumScore)
                    && ScoreUtil.equalsIgnoringScale(softScore, otherSoftScore);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(structuralScore, ScoreUtil.hashCodeIgnoringScale(hardScore),
                ScoreUtil.hashCodeIgnoringScale(mediumScore), ScoreUtil.hashCodeIgnoringScale(softScore));
    }

    @Override
    public int compareTo(HardMediumSoftBigDecimalScore other) {
        if (structuralScore != other.structuralScore) {
            return Long.compare(structuralScore, other.structuralScore);
        }
        var hardScoreComparison = hardScore.compareTo(other.hardScore());
        if (hardScoreComparison != 0) {
            return hardScoreComparison;
        }
        var mediumScoreComparison = mediumScore.compareTo(other.mediumScore());
        if (mediumScoreComparison != 0) {
            return mediumScoreComparison;
        } else {
            return softScore.compareTo(other.softScore());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, ScoreUtil.BIG_DECIMAL_NOT_ZERO,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return (structuralScore < 0)
                ? "%d%s/%s%s/%s%s/%s%s".formatted(structuralScore, STRUCTURAL_LABEL, hardScore, HARD_LABEL, mediumScore,
                        MEDIUM_LABEL,
                        softScore, SOFT_LABEL)
                : "%s%s/%s%s/%s%s".formatted(hardScore, HARD_LABEL, mediumScore, MEDIUM_LABEL, softScore, SOFT_LABEL);
    }

}
