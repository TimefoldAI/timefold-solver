package ai.timefold.solver.core.config.solver.termination;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbDurationAdapter;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "terminationClass",
        "terminationCompositionStyle",
        "spentLimit",
        "millisecondsSpentLimit",
        "secondsSpentLimit",
        "minutesSpentLimit",
        "hoursSpentLimit",
        "daysSpentLimit",
        "unimprovedSpentLimit",
        "unimprovedMillisecondsSpentLimit",
        "unimprovedSecondsSpentLimit",
        "unimprovedMinutesSpentLimit",
        "unimprovedHoursSpentLimit",
        "unimprovedDaysSpentLimit",
        "unimprovedScoreDifferenceThreshold",
        "bestScoreLimit",
        "bestScoreFeasible",
        "stepCountLimit",
        "unimprovedStepCountLimit",
        "scoreCalculationCountLimit",
        "moveCountLimit",
        "flatLineDetectionRatio",
        "newCurveDetectionRatio",
        "terminationConfigList"
})
public class TerminationConfig extends AbstractConfig<TerminationConfig> {

    /**
     * @deprecated A custom terminationClass is deprecated and will be removed in a future major version of Timefold.
     */
    @Deprecated(forRemoval = true)
    private Class<? extends Termination> terminationClass = null;

    private TerminationCompositionStyle terminationCompositionStyle = null;

    @XmlJavaTypeAdapter(JaxbDurationAdapter.class)
    private Duration spentLimit = null;
    private Long millisecondsSpentLimit = null;
    private Long secondsSpentLimit = null;
    private Long minutesSpentLimit = null;
    private Long hoursSpentLimit = null;
    private Long daysSpentLimit = null;

    @XmlJavaTypeAdapter(JaxbDurationAdapter.class)
    private Duration unimprovedSpentLimit = null;
    private Long unimprovedMillisecondsSpentLimit = null;
    private Long unimprovedSecondsSpentLimit = null;
    private Long unimprovedMinutesSpentLimit = null;
    private Long unimprovedHoursSpentLimit = null;
    private Long unimprovedDaysSpentLimit = null;
    private String unimprovedScoreDifferenceThreshold = null;

    private String bestScoreLimit = null;
    private Boolean bestScoreFeasible = null;

    private Integer stepCountLimit = null;
    private Integer unimprovedStepCountLimit = null;

    private Long scoreCalculationCountLimit = null;

    private Long moveCountLimit = null;

    private Double flatLineDetectionRatio = null;
    private Double newCurveDetectionRatio = null;

    @XmlElement(name = "termination")
    private List<TerminationConfig> terminationConfigList = null;

    /**
     * @deprecated A custom terminationClass is deprecated and will be removed in a future major version of Timefold.
     */
    @Deprecated(forRemoval = true)
    public Class<? extends Termination> getTerminationClass() {
        return terminationClass;
    }

    /**
     * @deprecated A custom terminationClass is deprecated and will be removed in a future major version of Timefold.
     */
    @Deprecated(forRemoval = true)
    public void setTerminationClass(Class<? extends Termination> terminationClass) {
        this.terminationClass = terminationClass;
    }

    public @Nullable TerminationCompositionStyle getTerminationCompositionStyle() {
        return terminationCompositionStyle;
    }

    public void setTerminationCompositionStyle(@Nullable TerminationCompositionStyle terminationCompositionStyle) {
        this.terminationCompositionStyle = terminationCompositionStyle;
    }

    public @Nullable Duration getSpentLimit() {
        return spentLimit;
    }

    public void setSpentLimit(@Nullable Duration spentLimit) {
        this.spentLimit = spentLimit;
    }

    public @Nullable Long getMillisecondsSpentLimit() {
        return millisecondsSpentLimit;
    }

    public void setMillisecondsSpentLimit(@Nullable Long millisecondsSpentLimit) {
        this.millisecondsSpentLimit = millisecondsSpentLimit;
    }

    public @Nullable Long getSecondsSpentLimit() {
        return secondsSpentLimit;
    }

    public void setSecondsSpentLimit(@Nullable Long secondsSpentLimit) {
        this.secondsSpentLimit = secondsSpentLimit;
    }

    public @Nullable Long getMinutesSpentLimit() {
        return minutesSpentLimit;
    }

    public void setMinutesSpentLimit(@Nullable Long minutesSpentLimit) {
        this.minutesSpentLimit = minutesSpentLimit;
    }

    public @Nullable Long getHoursSpentLimit() {
        return hoursSpentLimit;
    }

    public void setHoursSpentLimit(@Nullable Long hoursSpentLimit) {
        this.hoursSpentLimit = hoursSpentLimit;
    }

    public @Nullable Long getDaysSpentLimit() {
        return daysSpentLimit;
    }

    public void setDaysSpentLimit(@Nullable Long daysSpentLimit) {
        this.daysSpentLimit = daysSpentLimit;
    }

    public @Nullable Duration getUnimprovedSpentLimit() {
        return unimprovedSpentLimit;
    }

    public void setUnimprovedSpentLimit(@Nullable Duration unimprovedSpentLimit) {
        this.unimprovedSpentLimit = unimprovedSpentLimit;
    }

    public @Nullable Long getUnimprovedMillisecondsSpentLimit() {
        return unimprovedMillisecondsSpentLimit;
    }

    public void setUnimprovedMillisecondsSpentLimit(@Nullable Long unimprovedMillisecondsSpentLimit) {
        this.unimprovedMillisecondsSpentLimit = unimprovedMillisecondsSpentLimit;
    }

    public @Nullable Long getUnimprovedSecondsSpentLimit() {
        return unimprovedSecondsSpentLimit;
    }

    public void setUnimprovedSecondsSpentLimit(@Nullable Long unimprovedSecondsSpentLimit) {
        this.unimprovedSecondsSpentLimit = unimprovedSecondsSpentLimit;
    }

    public @Nullable Long getUnimprovedMinutesSpentLimit() {
        return unimprovedMinutesSpentLimit;
    }

    public void setUnimprovedMinutesSpentLimit(@Nullable Long unimprovedMinutesSpentLimit) {
        this.unimprovedMinutesSpentLimit = unimprovedMinutesSpentLimit;
    }

    public @Nullable Long getUnimprovedHoursSpentLimit() {
        return unimprovedHoursSpentLimit;
    }

    public void setUnimprovedHoursSpentLimit(@Nullable Long unimprovedHoursSpentLimit) {
        this.unimprovedHoursSpentLimit = unimprovedHoursSpentLimit;
    }

    public @Nullable Long getUnimprovedDaysSpentLimit() {
        return unimprovedDaysSpentLimit;
    }

    public void setUnimprovedDaysSpentLimit(@Nullable Long unimprovedDaysSpentLimit) {
        this.unimprovedDaysSpentLimit = unimprovedDaysSpentLimit;
    }

    public @Nullable String getUnimprovedScoreDifferenceThreshold() {
        return unimprovedScoreDifferenceThreshold;
    }

    public void setUnimprovedScoreDifferenceThreshold(@Nullable String unimprovedScoreDifferenceThreshold) {
        this.unimprovedScoreDifferenceThreshold = unimprovedScoreDifferenceThreshold;
    }

    public @Nullable String getBestScoreLimit() {
        return bestScoreLimit;
    }

    public void setBestScoreLimit(@Nullable String bestScoreLimit) {
        this.bestScoreLimit = bestScoreLimit;
    }

    public @Nullable Boolean getBestScoreFeasible() {
        return bestScoreFeasible;
    }

    public void setBestScoreFeasible(@Nullable Boolean bestScoreFeasible) {
        this.bestScoreFeasible = bestScoreFeasible;
    }

    public @Nullable Integer getStepCountLimit() {
        return stepCountLimit;
    }

    public void setStepCountLimit(@Nullable Integer stepCountLimit) {
        this.stepCountLimit = stepCountLimit;
    }

    public @Nullable Integer getUnimprovedStepCountLimit() {
        return unimprovedStepCountLimit;
    }

    public void setUnimprovedStepCountLimit(@Nullable Integer unimprovedStepCountLimit) {
        this.unimprovedStepCountLimit = unimprovedStepCountLimit;
    }

    public @Nullable Long getScoreCalculationCountLimit() {
        return scoreCalculationCountLimit;
    }

    public void setScoreCalculationCountLimit(@Nullable Long scoreCalculationCountLimit) {
        this.scoreCalculationCountLimit = scoreCalculationCountLimit;
    }

    public @Nullable Long getMoveCountLimit() {
        return moveCountLimit;
    }

    public void setMoveCountLimit(@Nullable Long moveCountLimit) {
        this.moveCountLimit = moveCountLimit;
    }

    public @Nullable Double getFlatLineDetectionRatio() {
        return flatLineDetectionRatio;
    }

    public void setFlatLineDetectionRatio(@Nullable Double flatLineDetectionRatio) {
        this.flatLineDetectionRatio = flatLineDetectionRatio;
    }

    public @Nullable Double getNewCurveDetectionRatio() {
        return newCurveDetectionRatio;
    }

    public void setNewCurveDetectionRatio(@Nullable Double newCurveDetectionRatio) {
        this.newCurveDetectionRatio = newCurveDetectionRatio;
    }

    public @Nullable List<@NonNull TerminationConfig> getTerminationConfigList() {
        return terminationConfigList;
    }

    public void setTerminationConfigList(@Nullable List<@NonNull TerminationConfig> terminationConfigList) {
        this.terminationConfigList = terminationConfigList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    /**
     * @deprecated A custom terminationClass is deprecated and will be removed in a future major version of Timefold.
     */
    @Deprecated(forRemoval = true)
    public TerminationConfig withTerminationClass(Class<? extends Termination> terminationClass) {
        this.terminationClass = terminationClass;
        return this;
    }

    public @NonNull TerminationConfig
            withTerminationCompositionStyle(@NonNull TerminationCompositionStyle terminationCompositionStyle) {
        this.terminationCompositionStyle = terminationCompositionStyle;
        return this;
    }

    public @NonNull TerminationConfig withSpentLimit(@NonNull Duration spentLimit) {
        this.spentLimit = spentLimit;
        return this;
    }

    public @NonNull TerminationConfig withMillisecondsSpentLimit(@NonNull Long millisecondsSpentLimit) {
        this.millisecondsSpentLimit = millisecondsSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withSecondsSpentLimit(@NonNull Long secondsSpentLimit) {
        this.secondsSpentLimit = secondsSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withMinutesSpentLimit(@NonNull Long minutesSpentLimit) {
        this.minutesSpentLimit = minutesSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withHoursSpentLimit(@NonNull Long hoursSpentLimit) {
        this.hoursSpentLimit = hoursSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withDaysSpentLimit(@NonNull Long daysSpentLimit) {
        this.daysSpentLimit = daysSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withUnimprovedSpentLimit(@NonNull Duration unimprovedSpentLimit) {
        this.unimprovedSpentLimit = unimprovedSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withUnimprovedMillisecondsSpentLimit(@NonNull Long unimprovedMillisecondsSpentLimit) {
        this.unimprovedMillisecondsSpentLimit = unimprovedMillisecondsSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withUnimprovedSecondsSpentLimit(@NonNull Long unimprovedSecondsSpentLimit) {
        this.unimprovedSecondsSpentLimit = unimprovedSecondsSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withUnimprovedMinutesSpentLimit(@NonNull Long unimprovedMinutesSpentLimit) {
        this.unimprovedMinutesSpentLimit = unimprovedMinutesSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withUnimprovedHoursSpentLimit(@NonNull Long unimprovedHoursSpentLimit) {
        this.unimprovedHoursSpentLimit = unimprovedHoursSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig withUnimprovedDaysSpentLimit(@NonNull Long unimprovedDaysSpentLimit) {
        this.unimprovedDaysSpentLimit = unimprovedDaysSpentLimit;
        return this;
    }

    public @NonNull TerminationConfig
            withUnimprovedScoreDifferenceThreshold(@NonNull String unimprovedScoreDifferenceThreshold) {
        this.unimprovedScoreDifferenceThreshold = unimprovedScoreDifferenceThreshold;
        return this;
    }

    public @NonNull TerminationConfig withBestScoreLimit(@NonNull String bestScoreLimit) {
        this.bestScoreLimit = bestScoreLimit;
        return this;
    }

    public @NonNull TerminationConfig withBestScoreFeasible(@NonNull Boolean bestScoreFeasible) {
        this.bestScoreFeasible = bestScoreFeasible;
        return this;
    }

    public @NonNull TerminationConfig withStepCountLimit(@NonNull Integer stepCountLimit) {
        this.stepCountLimit = stepCountLimit;
        return this;
    }

    public @NonNull TerminationConfig withUnimprovedStepCountLimit(@NonNull Integer unimprovedStepCountLimit) {
        this.unimprovedStepCountLimit = unimprovedStepCountLimit;
        return this;
    }

    public @NonNull TerminationConfig withScoreCalculationCountLimit(@NonNull Long scoreCalculationCountLimit) {
        this.scoreCalculationCountLimit = scoreCalculationCountLimit;
        return this;
    }

    public @NonNull TerminationConfig withMoveCountLimit(@NonNull Long moveCountLimit) {
        this.moveCountLimit = moveCountLimit;
        return this;
    }

    public @NonNull TerminationConfig withFlatLineDetectionRatio(@NonNull Double flatLineDetectionRatio) {
        this.flatLineDetectionRatio = flatLineDetectionRatio;
        return this;
    }

    public @NonNull TerminationConfig withNewCurveDetectionRatio(@NonNull Double newCurveDetectionRatio) {
        this.newCurveDetectionRatio = newCurveDetectionRatio;
        return this;
    }

    public @NonNull TerminationConfig
            withTerminationConfigList(@NonNull List<@NonNull TerminationConfig> terminationConfigList) {
        this.terminationConfigList = terminationConfigList;
        return this;
    }

    public void overwriteSpentLimit(@Nullable Duration spentLimit) {
        setSpentLimit(spentLimit);
        setMillisecondsSpentLimit(null);
        setSecondsSpentLimit(null);
        setMinutesSpentLimit(null);
        setHoursSpentLimit(null);
        setDaysSpentLimit(null);
    }

    public @Nullable Long calculateTimeMillisSpentLimit() {
        if (millisecondsSpentLimit == null && secondsSpentLimit == null
                && minutesSpentLimit == null && hoursSpentLimit == null && daysSpentLimit == null) {
            if (spentLimit != null) {
                if (spentLimit.getNano() % 1000 != 0) {
                    throw new IllegalArgumentException("The termination spentLimit (" + spentLimit
                            + ") cannot use nanoseconds.");
                }
                return spentLimit.toMillis();
            }
            return null;
        }
        if (spentLimit != null) {
            throw new IllegalArgumentException("The termination spentLimit (" + spentLimit
                    + ") cannot be combined with millisecondsSpentLimit (" + millisecondsSpentLimit
                    + "), secondsSpentLimit (" + secondsSpentLimit
                    + "), minutesSpentLimit (" + minutesSpentLimit
                    + "), hoursSpentLimit (" + hoursSpentLimit
                    + ") or daysSpentLimit (" + daysSpentLimit + ").");
        }
        long timeMillisSpentLimit = 0L
                + requireNonNegative(millisecondsSpentLimit, "millisecondsSpentLimit")
                + requireNonNegative(secondsSpentLimit, "secondsSpentLimit") * 1_000L
                + requireNonNegative(minutesSpentLimit, "minutesSpentLimit") * 60_000L
                + requireNonNegative(hoursSpentLimit, "hoursSpentLimit") * 3_600_000L
                + requireNonNegative(daysSpentLimit, "daysSpentLimit") * 86_400_000L;
        return timeMillisSpentLimit;
    }

    public void shortenTimeMillisSpentLimit(long timeMillisSpentLimit) {
        Long oldLimit = calculateTimeMillisSpentLimit();
        if (oldLimit == null || timeMillisSpentLimit < oldLimit) {
            spentLimit = null;
            millisecondsSpentLimit = timeMillisSpentLimit;
            secondsSpentLimit = null;
            minutesSpentLimit = null;
            hoursSpentLimit = null;
            daysSpentLimit = null;
        }
    }

    public void overwriteUnimprovedSpentLimit(@Nullable Duration unimprovedSpentLimit) {
        setUnimprovedSpentLimit(unimprovedSpentLimit);
        setUnimprovedMillisecondsSpentLimit(null);
        setUnimprovedSecondsSpentLimit(null);
        setUnimprovedMinutesSpentLimit(null);
        setUnimprovedHoursSpentLimit(null);
        setUnimprovedDaysSpentLimit(null);
    }

    public @Nullable Long calculateUnimprovedTimeMillisSpentLimit() {
        if (unimprovedMillisecondsSpentLimit == null && unimprovedSecondsSpentLimit == null
                && unimprovedMinutesSpentLimit == null && unimprovedHoursSpentLimit == null) {
            if (unimprovedSpentLimit != null) {
                if (unimprovedSpentLimit.getNano() % 1000 != 0) {
                    throw new IllegalArgumentException("The termination unimprovedSpentLimit (" + unimprovedSpentLimit
                            + ") cannot use nanoseconds.");
                }
                return unimprovedSpentLimit.toMillis();
            }
            return null;
        }
        if (unimprovedSpentLimit != null) {
            throw new IllegalArgumentException("The termination unimprovedSpentLimit (" + unimprovedSpentLimit
                    + ") cannot be combined with unimprovedMillisecondsSpentLimit (" + unimprovedMillisecondsSpentLimit
                    + "), unimprovedSecondsSpentLimit (" + unimprovedSecondsSpentLimit
                    + "), unimprovedMinutesSpentLimit (" + unimprovedMinutesSpentLimit
                    + "), unimprovedHoursSpentLimit (" + unimprovedHoursSpentLimit + ").");
        }
        long unimprovedTimeMillisSpentLimit = 0L
                + requireNonNegative(unimprovedMillisecondsSpentLimit, "unimprovedMillisecondsSpentLimit")
                + requireNonNegative(unimprovedSecondsSpentLimit, "unimprovedSecondsSpentLimit") * 1000L
                + requireNonNegative(unimprovedMinutesSpentLimit, "unimprovedMinutesSpentLimit") * 60_000L
                + requireNonNegative(unimprovedHoursSpentLimit, "unimprovedHoursSpentLimit") * 3_600_000L
                + requireNonNegative(unimprovedDaysSpentLimit, "unimprovedDaysSpentLimit") * 86_400_000L;
        return unimprovedTimeMillisSpentLimit;
    }

    /**
     * Return true if this TerminationConfig configures a termination condition.
     * Note: this does not mean it will always terminate: ex: bestScoreLimit configured,
     * but it is impossible to reach the bestScoreLimit.
     */
    @XmlTransient
    public boolean isConfigured() {
        return terminationClass != null ||
                timeSpentLimitIsSet() ||
                unimprovedTimeSpentLimitIsSet() ||
                bestScoreLimit != null ||
                bestScoreFeasible != null ||
                stepCountLimit != null ||
                unimprovedStepCountLimit != null ||
                scoreCalculationCountLimit != null ||
                moveCountLimit != null ||
                flatLineDetectionRatio != null ||
                newCurveDetectionRatio != null ||
                isTerminationListConfigured();
    }

    private boolean isTerminationListConfigured() {
        if (terminationConfigList == null || terminationCompositionStyle == null) {
            return false;
        }

        return switch (terminationCompositionStyle) {
            case AND -> terminationConfigList.stream().allMatch(TerminationConfig::isConfigured);
            case OR -> terminationConfigList.stream().anyMatch(TerminationConfig::isConfigured);
        };
    }

    @Override
    public @NonNull TerminationConfig inherit(@NonNull TerminationConfig inheritedConfig) {
        if (!timeSpentLimitIsSet()) {
            inheritTimeSpentLimit(inheritedConfig);
        }
        if (!unimprovedTimeSpentLimitIsSet()) {
            inheritUnimprovedTimeSpentLimit(inheritedConfig);
        }
        terminationClass = ConfigUtils.inheritOverwritableProperty(terminationClass,
                inheritedConfig.getTerminationClass());
        terminationCompositionStyle = ConfigUtils.inheritOverwritableProperty(terminationCompositionStyle,
                inheritedConfig.getTerminationCompositionStyle());
        unimprovedScoreDifferenceThreshold = ConfigUtils.inheritOverwritableProperty(unimprovedScoreDifferenceThreshold,
                inheritedConfig.getUnimprovedScoreDifferenceThreshold());
        bestScoreLimit = ConfigUtils.inheritOverwritableProperty(bestScoreLimit,
                inheritedConfig.getBestScoreLimit());
        bestScoreFeasible = ConfigUtils.inheritOverwritableProperty(bestScoreFeasible,
                inheritedConfig.getBestScoreFeasible());
        stepCountLimit = ConfigUtils.inheritOverwritableProperty(stepCountLimit,
                inheritedConfig.getStepCountLimit());
        unimprovedStepCountLimit = ConfigUtils.inheritOverwritableProperty(unimprovedStepCountLimit,
                inheritedConfig.getUnimprovedStepCountLimit());
        scoreCalculationCountLimit = ConfigUtils.inheritOverwritableProperty(scoreCalculationCountLimit,
                inheritedConfig.getScoreCalculationCountLimit());
        moveCountLimit = ConfigUtils.inheritOverwritableProperty(moveCountLimit,
                inheritedConfig.getMoveCountLimit());
        flatLineDetectionRatio = ConfigUtils.inheritOverwritableProperty(flatLineDetectionRatio,
                inheritedConfig.getFlatLineDetectionRatio());
        newCurveDetectionRatio = ConfigUtils.inheritOverwritableProperty(newCurveDetectionRatio,
                inheritedConfig.getNewCurveDetectionRatio());
        terminationConfigList = ConfigUtils.inheritMergeableListConfig(
                terminationConfigList, inheritedConfig.getTerminationConfigList());
        return this;
    }

    @Override
    public @NonNull TerminationConfig copyConfig() {
        return new TerminationConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(terminationClass);
        if (terminationConfigList != null) {
            terminationConfigList.forEach(tc -> tc.visitReferencedClasses(classVisitor));
        }
    }

    private TerminationConfig inheritTimeSpentLimit(TerminationConfig parent) {
        spentLimit = ConfigUtils.inheritOverwritableProperty(spentLimit,
                parent.getSpentLimit());
        millisecondsSpentLimit = ConfigUtils.inheritOverwritableProperty(millisecondsSpentLimit,
                parent.getMillisecondsSpentLimit());
        secondsSpentLimit = ConfigUtils.inheritOverwritableProperty(secondsSpentLimit,
                parent.getSecondsSpentLimit());
        minutesSpentLimit = ConfigUtils.inheritOverwritableProperty(minutesSpentLimit,
                parent.getMinutesSpentLimit());
        hoursSpentLimit = ConfigUtils.inheritOverwritableProperty(hoursSpentLimit,
                parent.getHoursSpentLimit());
        daysSpentLimit = ConfigUtils.inheritOverwritableProperty(daysSpentLimit,
                parent.getDaysSpentLimit());
        return this;
    }

    private TerminationConfig inheritUnimprovedTimeSpentLimit(TerminationConfig parent) {
        unimprovedSpentLimit = ConfigUtils.inheritOverwritableProperty(unimprovedSpentLimit,
                parent.getUnimprovedSpentLimit());
        unimprovedMillisecondsSpentLimit = ConfigUtils.inheritOverwritableProperty(unimprovedMillisecondsSpentLimit,
                parent.getUnimprovedMillisecondsSpentLimit());
        unimprovedSecondsSpentLimit = ConfigUtils.inheritOverwritableProperty(unimprovedSecondsSpentLimit,
                parent.getUnimprovedSecondsSpentLimit());
        unimprovedMinutesSpentLimit = ConfigUtils.inheritOverwritableProperty(unimprovedMinutesSpentLimit,
                parent.getUnimprovedMinutesSpentLimit());
        unimprovedHoursSpentLimit = ConfigUtils.inheritOverwritableProperty(unimprovedHoursSpentLimit,
                parent.getUnimprovedHoursSpentLimit());
        unimprovedDaysSpentLimit = ConfigUtils.inheritOverwritableProperty(unimprovedDaysSpentLimit,
                parent.getUnimprovedDaysSpentLimit());
        return this;
    }

    /**
     * Assert that the parameter is non-negative and return its value,
     * converting {@code null} to 0.
     *
     * @param param the parameter to test/convert
     * @param name the name of the parameter, for use in the exception message
     * @throws IllegalArgumentException iff param is negative
     */
    private Long requireNonNegative(Long param, String name) {
        if (param == null) {
            return 0L; // Makes adding a null param a NOP.
        } else if (param < 0L) {
            String msg = String.format("The termination %s (%d) cannot be negative.", name, param);
            throw new IllegalArgumentException(msg);
        } else {
            return param;
        }
    }

    /** Check whether any ...SpentLimit is non-null. */
    private boolean timeSpentLimitIsSet() {
        return getDaysSpentLimit() != null
                || getHoursSpentLimit() != null
                || getMinutesSpentLimit() != null
                || getSecondsSpentLimit() != null
                || getMillisecondsSpentLimit() != null
                || getSpentLimit() != null;
    }

    /** Check whether any unimproved...SpentLimit is non-null. */
    private boolean unimprovedTimeSpentLimitIsSet() {
        return getUnimprovedDaysSpentLimit() != null
                || getUnimprovedHoursSpentLimit() != null
                || getUnimprovedMinutesSpentLimit() != null
                || getUnimprovedSecondsSpentLimit() != null
                || getUnimprovedMillisecondsSpentLimit() != null
                || getUnimprovedSpentLimit() != null;
    }

}
