package ai.timefold.solver.core.config.constructionheuristic;

import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicForagerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.PooledEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

@XmlType(propOrder = {
        "constructionHeuristicType",
        "entitySorterManner",
        "valueSorterManner",
        "entityPlacerConfig",
        "moveSelectorConfigList",
        "foragerConfig"
})
public class ConstructionHeuristicPhaseConfig extends PhaseConfig<ConstructionHeuristicPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "constructionHeuristic";

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    protected ConstructionHeuristicType constructionHeuristicType = null;
    protected EntitySorterManner entitySorterManner = null;
    protected ValueSorterManner valueSorterManner = null;

    @XmlElements({
            @XmlElement(name = "queuedEntityPlacer", type = QueuedEntityPlacerConfig.class),
            @XmlElement(name = "queuedValuePlacer", type = QueuedValuePlacerConfig.class),
            @XmlElement(name = "pooledEntityPlacer", type = PooledEntityPlacerConfig.class)
    })
    protected EntityPlacerConfig entityPlacerConfig = null;

    /** Simpler alternative for {@link #entityPlacerConfig}. */
    @XmlElements({
            @XmlElement(name = CartesianProductMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = CartesianProductMoveSelectorConfig.class),
            @XmlElement(name = ChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = ChangeMoveSelectorConfig.class),
            @XmlElement(name = MoveIteratorFactoryConfig.XML_ELEMENT_NAME, type = MoveIteratorFactoryConfig.class),
            @XmlElement(name = MoveListFactoryConfig.XML_ELEMENT_NAME, type = MoveListFactoryConfig.class),
            @XmlElement(name = PillarChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = PillarChangeMoveSelectorConfig.class),
            @XmlElement(name = PillarSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = PillarSwapMoveSelectorConfig.class),
            @XmlElement(name = SubChainChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainChangeMoveSelectorConfig.class),
            @XmlElement(name = SubChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainSwapMoveSelectorConfig.class),
            @XmlElement(name = SwapMoveSelectorConfig.XML_ELEMENT_NAME, type = SwapMoveSelectorConfig.class),
            @XmlElement(name = TailChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = TailChainSwapMoveSelectorConfig.class),
            @XmlElement(name = UnionMoveSelectorConfig.XML_ELEMENT_NAME, type = UnionMoveSelectorConfig.class)
    })
    protected List<MoveSelectorConfig> moveSelectorConfigList = null;

    @XmlElement(name = "forager")
    protected ConstructionHeuristicForagerConfig foragerConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ConstructionHeuristicType getConstructionHeuristicType() {
        return constructionHeuristicType;
    }

    public void setConstructionHeuristicType(ConstructionHeuristicType constructionHeuristicType) {
        this.constructionHeuristicType = constructionHeuristicType;
    }

    public EntitySorterManner getEntitySorterManner() {
        return entitySorterManner;
    }

    public void setEntitySorterManner(EntitySorterManner entitySorterManner) {
        this.entitySorterManner = entitySorterManner;
    }

    public ValueSorterManner getValueSorterManner() {
        return valueSorterManner;
    }

    public void setValueSorterManner(ValueSorterManner valueSorterManner) {
        this.valueSorterManner = valueSorterManner;
    }

    public EntityPlacerConfig getEntityPlacerConfig() {
        return entityPlacerConfig;
    }

    public void setEntityPlacerConfig(EntityPlacerConfig entityPlacerConfig) {
        this.entityPlacerConfig = entityPlacerConfig;
    }

    public List<MoveSelectorConfig> getMoveSelectorConfigList() {
        return moveSelectorConfigList;
    }

    public void setMoveSelectorConfigList(List<MoveSelectorConfig> moveSelectorConfigList) {
        this.moveSelectorConfigList = moveSelectorConfigList;
    }

    public ConstructionHeuristicForagerConfig getForagerConfig() {
        return foragerConfig;
    }

    public void setForagerConfig(ConstructionHeuristicForagerConfig foragerConfig) {
        this.foragerConfig = foragerConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public ConstructionHeuristicPhaseConfig withConstructionHeuristicType(
            ConstructionHeuristicType constructionHeuristicType) {
        this.constructionHeuristicType = constructionHeuristicType;
        return this;
    }

    public ConstructionHeuristicPhaseConfig withEntitySorterManner(EntitySorterManner entitySorterManner) {
        this.entitySorterManner = entitySorterManner;
        return this;
    }

    public ConstructionHeuristicPhaseConfig withValueSorterManner(ValueSorterManner valueSorterManner) {
        this.valueSorterManner = valueSorterManner;
        return this;
    }

    public ConstructionHeuristicPhaseConfig withEntityPlacerConfig(EntityPlacerConfig<?> entityPlacerConfig) {
        this.entityPlacerConfig = entityPlacerConfig;
        return this;
    }

    public ConstructionHeuristicPhaseConfig withMoveSelectorConfigList(List<MoveSelectorConfig> moveSelectorConfigList) {
        this.moveSelectorConfigList = moveSelectorConfigList;
        return this;
    }

    public ConstructionHeuristicPhaseConfig withForagerConfig(ConstructionHeuristicForagerConfig foragerConfig) {
        this.foragerConfig = foragerConfig;
        return this;
    }

    @Override
    public ConstructionHeuristicPhaseConfig inherit(ConstructionHeuristicPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        constructionHeuristicType = ConfigUtils.inheritOverwritableProperty(constructionHeuristicType,
                inheritedConfig.getConstructionHeuristicType());
        entitySorterManner = ConfigUtils.inheritOverwritableProperty(entitySorterManner,
                inheritedConfig.getEntitySorterManner());
        valueSorterManner = ConfigUtils.inheritOverwritableProperty(valueSorterManner,
                inheritedConfig.getValueSorterManner());
        setEntityPlacerConfig(ConfigUtils.inheritOverwritableProperty(
                getEntityPlacerConfig(), inheritedConfig.getEntityPlacerConfig()));
        moveSelectorConfigList = ConfigUtils.inheritMergeableListConfig(
                moveSelectorConfigList, inheritedConfig.getMoveSelectorConfigList());
        foragerConfig = ConfigUtils.inheritConfig(foragerConfig, inheritedConfig.getForagerConfig());
        return this;
    }

    @Override
    public ConstructionHeuristicPhaseConfig copyConfig() {
        return new ConstructionHeuristicPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        if (getTerminationConfig() != null) {
            getTerminationConfig().visitReferencedClasses(classVisitor);
        }
        if (entityPlacerConfig != null) {
            entityPlacerConfig.visitReferencedClasses(classVisitor);
        }
        if (moveSelectorConfigList != null) {
            moveSelectorConfigList.forEach(ms -> ms.visitReferencedClasses(classVisitor));
        }
        if (foragerConfig != null) {
            foragerConfig.visitReferencedClasses(classVisitor);
        }
    }

}
