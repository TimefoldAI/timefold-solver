package ai.timefold.solver.core.config.localsearch.decider.acceptor;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;

@XmlType(propOrder = {
        "moveCountLimitPercentage",
        "reconfigurationRatio"
})
public class ReconfigurationConfig extends AbstractConfig<ReconfigurationConfig> {

    private Double moveCountLimitPercentage = null;
    private Double reconfigurationRatio = null;

    public Double getMoveCountLimitPercentage() {
        return moveCountLimitPercentage;
    }

    public void setMoveCountLimitPercentage(Double moveCountLimitPercentage) {
        this.moveCountLimitPercentage = moveCountLimitPercentage;
    }

    public Double getReconfigurationRatio() {
        return reconfigurationRatio;
    }

    public void setReconfigurationRatio(Double reconfigurationRatio) {
        this.reconfigurationRatio = reconfigurationRatio;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public ReconfigurationConfig withMoveCountLimitPercentage(Double moveCountLimitPercentage) {
        this.moveCountLimitPercentage = moveCountLimitPercentage;
        return this;
    }

    public ReconfigurationConfig withReconfigurationRatio(Double reconfigurationRatio) {
        this.reconfigurationRatio = reconfigurationRatio;
        return this;
    }

    @Override
    public ReconfigurationConfig inherit(ReconfigurationConfig inheritedConfig) {
        moveCountLimitPercentage = inheritedConfig.getMoveCountLimitPercentage();
        reconfigurationRatio = inheritedConfig.getReconfigurationRatio();
        return this;
    }

    @Override
    public ReconfigurationConfig copyConfig() {
        return new ReconfigurationConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        // No referenced classes
    }
}
