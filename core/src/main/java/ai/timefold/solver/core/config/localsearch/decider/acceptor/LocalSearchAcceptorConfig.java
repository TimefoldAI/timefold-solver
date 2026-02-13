package ai.timefold.solver.core.config.localsearch.decider.acceptor;

import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.stepcountinghillclimbing.StepCountingHillClimbingType;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "acceptorTypeList",
        "entityTabuSize",
        "entityTabuRatio",
        "fadingEntityTabuSize",
        "fadingEntityTabuRatio",
        "valueTabuSize",
        "fadingValueTabuSize",
        "moveTabuSize",
        "fadingMoveTabuSize",
        "simulatedAnnealingStartingTemperature",
        "lateAcceptanceSize",
        "greatDelugeWaterLevelIncrementScore",
        "greatDelugeWaterLevelIncrementRatio",
        "stepCountingHillClimbingSize",
        "stepCountingHillClimbingType"
})
public class LocalSearchAcceptorConfig extends AbstractConfig<LocalSearchAcceptorConfig> {

    @XmlElement(name = "acceptorType")
    private List<AcceptorType> acceptorTypeList = null;

    protected Integer entityTabuSize = null;
    protected Double entityTabuRatio = null;
    protected Integer fadingEntityTabuSize = null;
    protected Double fadingEntityTabuRatio = null;
    protected Integer valueTabuSize = null;
    protected Integer fadingValueTabuSize = null;
    protected Integer moveTabuSize = null;
    protected Integer fadingMoveTabuSize = null;

    protected String simulatedAnnealingStartingTemperature = null;

    protected Integer lateAcceptanceSize = null;

    protected String greatDelugeWaterLevelIncrementScore = null;
    protected Double greatDelugeWaterLevelIncrementRatio = null;

    protected Integer stepCountingHillClimbingSize = null;
    protected StepCountingHillClimbingType stepCountingHillClimbingType = null;

    public @Nullable List<AcceptorType> getAcceptorTypeList() {
        return acceptorTypeList;
    }

    public void setAcceptorTypeList(@Nullable List<AcceptorType> acceptorTypeList) {
        this.acceptorTypeList = acceptorTypeList;
    }

    public @Nullable Integer getEntityTabuSize() {
        return entityTabuSize;
    }

    public void setEntityTabuSize(@Nullable Integer entityTabuSize) {
        this.entityTabuSize = entityTabuSize;
    }

    public @Nullable Double getEntityTabuRatio() {
        return entityTabuRatio;
    }

    public void setEntityTabuRatio(@Nullable Double entityTabuRatio) {
        this.entityTabuRatio = entityTabuRatio;
    }

    public @Nullable Integer getFadingEntityTabuSize() {
        return fadingEntityTabuSize;
    }

    public void setFadingEntityTabuSize(@Nullable Integer fadingEntityTabuSize) {
        this.fadingEntityTabuSize = fadingEntityTabuSize;
    }

    public @Nullable Double getFadingEntityTabuRatio() {
        return fadingEntityTabuRatio;
    }

    public void setFadingEntityTabuRatio(@Nullable Double fadingEntityTabuRatio) {
        this.fadingEntityTabuRatio = fadingEntityTabuRatio;
    }

    public @Nullable Integer getValueTabuSize() {
        return valueTabuSize;
    }

    public void setValueTabuSize(@Nullable Integer valueTabuSize) {
        this.valueTabuSize = valueTabuSize;
    }

    public @Nullable Integer getFadingValueTabuSize() {
        return fadingValueTabuSize;
    }

    public void setFadingValueTabuSize(@Nullable Integer fadingValueTabuSize) {
        this.fadingValueTabuSize = fadingValueTabuSize;
    }

    public @Nullable Integer getMoveTabuSize() {
        return moveTabuSize;
    }

    public void setMoveTabuSize(@Nullable Integer moveTabuSize) {
        this.moveTabuSize = moveTabuSize;
    }

    public @Nullable Integer getFadingMoveTabuSize() {
        return fadingMoveTabuSize;
    }

    public void setFadingMoveTabuSize(@Nullable Integer fadingMoveTabuSize) {
        this.fadingMoveTabuSize = fadingMoveTabuSize;
    }

    public @Nullable String getSimulatedAnnealingStartingTemperature() {
        return simulatedAnnealingStartingTemperature;
    }

    public void setSimulatedAnnealingStartingTemperature(@Nullable String simulatedAnnealingStartingTemperature) {
        this.simulatedAnnealingStartingTemperature = simulatedAnnealingStartingTemperature;
    }

    public @Nullable Integer getLateAcceptanceSize() {
        return lateAcceptanceSize;
    }

    public void setLateAcceptanceSize(@Nullable Integer lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public @Nullable String getGreatDelugeWaterLevelIncrementScore() {
        return greatDelugeWaterLevelIncrementScore;
    }

    public void setGreatDelugeWaterLevelIncrementScore(@Nullable String greatDelugeWaterLevelIncrementScore) {
        this.greatDelugeWaterLevelIncrementScore = greatDelugeWaterLevelIncrementScore;
    }

    public @Nullable Double getGreatDelugeWaterLevelIncrementRatio() {
        return greatDelugeWaterLevelIncrementRatio;
    }

    public void setGreatDelugeWaterLevelIncrementRatio(@Nullable Double greatDelugeWaterLevelIncrementRatio) {
        this.greatDelugeWaterLevelIncrementRatio = greatDelugeWaterLevelIncrementRatio;
    }

    public @Nullable Integer getStepCountingHillClimbingSize() {
        return stepCountingHillClimbingSize;
    }

    public void setStepCountingHillClimbingSize(@Nullable Integer stepCountingHillClimbingSize) {
        this.stepCountingHillClimbingSize = stepCountingHillClimbingSize;
    }

    public @Nullable StepCountingHillClimbingType getStepCountingHillClimbingType() {
        return stepCountingHillClimbingType;
    }

    public void setStepCountingHillClimbingType(@Nullable StepCountingHillClimbingType stepCountingHillClimbingType) {
        this.stepCountingHillClimbingType = stepCountingHillClimbingType;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull LocalSearchAcceptorConfig withAcceptorTypeList(@NonNull List<AcceptorType> acceptorTypeList) {
        this.acceptorTypeList = acceptorTypeList;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withEntityTabuSize(@NonNull Integer entityTabuSize) {
        this.entityTabuSize = entityTabuSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withEntityTabuRatio(@NonNull Double entityTabuRatio) {
        this.entityTabuRatio = entityTabuRatio;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withFadingEntityTabuSize(@NonNull Integer fadingEntityTabuSize) {
        this.fadingEntityTabuSize = fadingEntityTabuSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withFadingEntityTabuRatio(@NonNull Double fadingEntityTabuRatio) {
        this.fadingEntityTabuRatio = fadingEntityTabuRatio;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withValueTabuSize(@NonNull Integer valueTabuSize) {
        this.valueTabuSize = valueTabuSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withFadingValueTabuSize(@NonNull Integer fadingValueTabuSize) {
        this.fadingValueTabuSize = fadingValueTabuSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withMoveTabuSize(@NonNull Integer moveTabuSize) {
        this.moveTabuSize = moveTabuSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withFadingMoveTabuSize(@NonNull Integer fadingMoveTabuSize) {
        this.fadingMoveTabuSize = fadingMoveTabuSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig
            withSimulatedAnnealingStartingTemperature(@NonNull String simulatedAnnealingStartingTemperature) {
        this.simulatedAnnealingStartingTemperature = simulatedAnnealingStartingTemperature;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withLateAcceptanceSize(@NonNull Integer lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig withStepCountingHillClimbingSize(@NonNull Integer stepCountingHillClimbingSize) {
        this.stepCountingHillClimbingSize = stepCountingHillClimbingSize;
        return this;
    }

    public @NonNull LocalSearchAcceptorConfig
            withStepCountingHillClimbingType(@NonNull StepCountingHillClimbingType stepCountingHillClimbingType) {
        this.stepCountingHillClimbingType = stepCountingHillClimbingType;
        return this;
    }

    @Override
    public @NonNull LocalSearchAcceptorConfig inherit(@NonNull LocalSearchAcceptorConfig inheritedConfig) {
        if (acceptorTypeList == null) {
            acceptorTypeList = inheritedConfig.getAcceptorTypeList();
        } else {
            List<AcceptorType> inheritedAcceptorTypeList = inheritedConfig.getAcceptorTypeList();
            if (inheritedAcceptorTypeList != null) {
                for (AcceptorType acceptorType : inheritedAcceptorTypeList) {
                    if (!acceptorTypeList.contains(acceptorType)) {
                        acceptorTypeList.add(acceptorType);
                    }
                }
            }
        }
        entityTabuSize = ConfigUtils.inheritOverwritableProperty(entityTabuSize, inheritedConfig.getEntityTabuSize());
        entityTabuRatio = ConfigUtils.inheritOverwritableProperty(entityTabuRatio, inheritedConfig.getEntityTabuRatio());
        fadingEntityTabuSize = ConfigUtils.inheritOverwritableProperty(fadingEntityTabuSize,
                inheritedConfig.getFadingEntityTabuSize());
        fadingEntityTabuRatio = ConfigUtils.inheritOverwritableProperty(fadingEntityTabuRatio,
                inheritedConfig.getFadingEntityTabuRatio());
        valueTabuSize = ConfigUtils.inheritOverwritableProperty(valueTabuSize, inheritedConfig.getValueTabuSize());
        fadingValueTabuSize = ConfigUtils.inheritOverwritableProperty(fadingValueTabuSize,
                inheritedConfig.getFadingValueTabuSize());
        moveTabuSize = ConfigUtils.inheritOverwritableProperty(moveTabuSize, inheritedConfig.getMoveTabuSize());
        fadingMoveTabuSize = ConfigUtils.inheritOverwritableProperty(fadingMoveTabuSize,
                inheritedConfig.getFadingMoveTabuSize());
        simulatedAnnealingStartingTemperature = ConfigUtils.inheritOverwritableProperty(
                simulatedAnnealingStartingTemperature, inheritedConfig.getSimulatedAnnealingStartingTemperature());
        lateAcceptanceSize = ConfigUtils.inheritOverwritableProperty(lateAcceptanceSize,
                inheritedConfig.getLateAcceptanceSize());
        greatDelugeWaterLevelIncrementScore = ConfigUtils.inheritOverwritableProperty(greatDelugeWaterLevelIncrementScore,
                inheritedConfig.getGreatDelugeWaterLevelIncrementScore());
        greatDelugeWaterLevelIncrementRatio = ConfigUtils.inheritOverwritableProperty(greatDelugeWaterLevelIncrementRatio,
                inheritedConfig.getGreatDelugeWaterLevelIncrementRatio());
        stepCountingHillClimbingSize = ConfigUtils.inheritOverwritableProperty(stepCountingHillClimbingSize,
                inheritedConfig.getStepCountingHillClimbingSize());
        stepCountingHillClimbingType = ConfigUtils.inheritOverwritableProperty(stepCountingHillClimbingType,
                inheritedConfig.getStepCountingHillClimbingType());
        return this;
    }

    @Override
    public @NonNull LocalSearchAcceptorConfig copyConfig() {
        return new LocalSearchAcceptorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        // No referenced classes
    }

}
