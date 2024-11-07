package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "secondaryPillarSelectorConfig",
        "variableNameIncludeList"
})
public class PillarSwapMoveSelectorConfig extends AbstractPillarMoveSelectorConfig<PillarSwapMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "pillarSwapMoveSelector";

    @XmlElement(name = "secondaryPillarSelector")
    private PillarSelectorConfig secondaryPillarSelectorConfig = null;

    @XmlElementWrapper(name = "variableNameIncludes")
    @XmlElement(name = "variableNameInclude")
    private List<String> variableNameIncludeList = null;

    public @Nullable PillarSelectorConfig getSecondaryPillarSelectorConfig() {
        return secondaryPillarSelectorConfig;
    }

    public void setSecondaryPillarSelectorConfig(@Nullable PillarSelectorConfig secondaryPillarSelectorConfig) {
        this.secondaryPillarSelectorConfig = secondaryPillarSelectorConfig;
    }

    public @Nullable List<@NonNull String> getVariableNameIncludeList() {
        return variableNameIncludeList;
    }

    public void setVariableNameIncludeList(@Nullable List<@NonNull String> variableNameIncludeList) {
        this.variableNameIncludeList = variableNameIncludeList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull PillarSwapMoveSelectorConfig
            withSecondaryPillarSelectorConfig(@NonNull PillarSelectorConfig pillarSelectorConfig) {
        this.setSecondaryPillarSelectorConfig(pillarSelectorConfig);
        return this;
    }

    public @NonNull PillarSwapMoveSelectorConfig
            withVariableNameIncludeList(@NonNull List<@NonNull String> variableNameIncludeList) {
        this.setVariableNameIncludeList(variableNameIncludeList);
        return this;
    }

    public @NonNull PillarSwapMoveSelectorConfig withVariableNameIncludes(@NonNull String @NonNull... variableNameIncludes) {
        this.setVariableNameIncludeList(List.of(variableNameIncludes));
        return this;
    }

    @Override
    public @NonNull PillarSwapMoveSelectorConfig inherit(@NonNull PillarSwapMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        secondaryPillarSelectorConfig = ConfigUtils.inheritConfig(secondaryPillarSelectorConfig,
                inheritedConfig.getSecondaryPillarSelectorConfig());
        variableNameIncludeList = ConfigUtils.inheritMergeableListProperty(
                variableNameIncludeList, inheritedConfig.getVariableNameIncludeList());
        return this;
    }

    @Override
    public @NonNull PillarSwapMoveSelectorConfig copyConfig() {
        return new PillarSwapMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (secondaryPillarSelectorConfig != null) {
            secondaryPillarSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (pillarSelectorConfig != null && pillarSelectorConfig.hasNearbySelectionConfig())
                || (secondaryPillarSelectorConfig != null && secondaryPillarSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + pillarSelectorConfig
                + (secondaryPillarSelectorConfig == null ? "" : ", " + secondaryPillarSelectorConfig) + ")";
    }

}
