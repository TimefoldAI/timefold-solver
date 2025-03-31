package ai.timefold.solver.core.config.localsearch;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProviders;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "localSearchType",
        "moveSelectorConfig",
        "moveProvidersClass",
        "acceptorConfig",
        "foragerConfig"
})
public class LocalSearchPhaseConfig extends PhaseConfig<LocalSearchPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "localSearch";

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    protected LocalSearchType localSearchType = null;

    @XmlElements({
            @XmlElement(name = CartesianProductMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = CartesianProductMoveSelectorConfig.class),
            @XmlElement(name = ChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = ChangeMoveSelectorConfig.class),
            @XmlElement(name = ListChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = ListChangeMoveSelectorConfig.class),
            @XmlElement(name = ListSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = ListSwapMoveSelectorConfig.class),
            @XmlElement(name = MoveIteratorFactoryConfig.XML_ELEMENT_NAME, type = MoveIteratorFactoryConfig.class),
            @XmlElement(name = MoveListFactoryConfig.XML_ELEMENT_NAME, type = MoveListFactoryConfig.class),
            @XmlElement(name = PillarChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = PillarChangeMoveSelectorConfig.class),
            @XmlElement(name = PillarSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = PillarSwapMoveSelectorConfig.class),
            @XmlElement(name = RuinRecreateMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = RuinRecreateMoveSelectorConfig.class),
            @XmlElement(name = ListRuinRecreateMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = ListRuinRecreateMoveSelectorConfig.class),
            @XmlElement(name = SubChainChangeMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainChangeMoveSelectorConfig.class),
            @XmlElement(name = SubChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = SubChainSwapMoveSelectorConfig.class),
            @XmlElement(name = SubListChangeMoveSelectorConfig.XML_ELEMENT_NAME, type = SubListChangeMoveSelectorConfig.class),
            @XmlElement(name = SubListSwapMoveSelectorConfig.XML_ELEMENT_NAME, type = SubListSwapMoveSelectorConfig.class),
            @XmlElement(name = SwapMoveSelectorConfig.XML_ELEMENT_NAME, type = SwapMoveSelectorConfig.class),
            @XmlElement(name = TailChainSwapMoveSelectorConfig.XML_ELEMENT_NAME,
                    type = TailChainSwapMoveSelectorConfig.class),
            @XmlElement(name = UnionMoveSelectorConfig.XML_ELEMENT_NAME, type = UnionMoveSelectorConfig.class)
    })
    private MoveSelectorConfig moveSelectorConfig = null;
    private Class<? extends MoveProviders> moveProvidersClass = null;
    @XmlElement(name = "acceptor")
    private LocalSearchAcceptorConfig acceptorConfig = null;
    @XmlElement(name = "forager")
    private LocalSearchForagerConfig foragerConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable LocalSearchType getLocalSearchType() {
        return localSearchType;
    }

    public void setLocalSearchType(@Nullable LocalSearchType localSearchType) {
        this.localSearchType = localSearchType;
    }

    public @Nullable MoveSelectorConfig getMoveSelectorConfig() {
        return moveSelectorConfig;
    }

    public void setMoveSelectorConfig(@Nullable MoveSelectorConfig moveSelectorConfig) {
        this.moveSelectorConfig = moveSelectorConfig;
    }

    /**
     * Part of {@link PreviewFeature#MOVE_STREAMS}.
     */
    @SuppressWarnings("unchecked")
    public @Nullable <Solution_> Class<? extends MoveProviders<Solution_>> getMoveProvidersClass() {
        return (Class<? extends MoveProviders<Solution_>>) moveProvidersClass;
    }

    /**
     * Part of {@link PreviewFeature#MOVE_STREAMS}.
     */
    @SuppressWarnings("rawtypes")
    public void setMoveProvidersClass(@Nullable Class<? extends MoveProviders> moveProvidersClass) {
        this.moveProvidersClass = moveProvidersClass;
    }

    public @Nullable LocalSearchAcceptorConfig getAcceptorConfig() {
        return acceptorConfig;
    }

    public void setAcceptorConfig(@Nullable LocalSearchAcceptorConfig acceptorConfig) {
        this.acceptorConfig = acceptorConfig;
    }

    public @Nullable LocalSearchForagerConfig getForagerConfig() {
        return foragerConfig;
    }

    public void setForagerConfig(@Nullable LocalSearchForagerConfig foragerConfig) {
        this.foragerConfig = foragerConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull LocalSearchPhaseConfig withLocalSearchType(@NonNull LocalSearchType localSearchType) {
        this.localSearchType = localSearchType;
        return this;
    }

    public @NonNull LocalSearchPhaseConfig withMoveSelectorConfig(@NonNull MoveSelectorConfig moveSelectorConfig) {
        this.moveSelectorConfig = moveSelectorConfig;
        return this;
    }

    /**
     * Part of {@link PreviewFeature#MOVE_STREAMS}.
     */
    public @NonNull LocalSearchPhaseConfig
            withMoveProvidersClass(@NonNull Class<? extends MoveProviders<?>> moveProviderClass) {
        this.moveProvidersClass = moveProviderClass;
        return this;
    }

    public @NonNull LocalSearchPhaseConfig withAcceptorConfig(@NonNull LocalSearchAcceptorConfig acceptorConfig) {
        this.acceptorConfig = acceptorConfig;
        return this;
    }

    public @NonNull LocalSearchPhaseConfig withForagerConfig(@NonNull LocalSearchForagerConfig foragerConfig) {
        this.foragerConfig = foragerConfig;
        return this;
    }

    @Override
    public @NonNull LocalSearchPhaseConfig inherit(@NonNull LocalSearchPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        localSearchType = ConfigUtils.inheritOverwritableProperty(localSearchType,
                inheritedConfig.getLocalSearchType());
        setMoveSelectorConfig(ConfigUtils.inheritOverwritableProperty(
                getMoveSelectorConfig(), inheritedConfig.getMoveSelectorConfig()));
        setMoveProvidersClass(ConfigUtils.inheritOverwritableProperty(getMoveProvidersClass(),
                inheritedConfig.getMoveProvidersClass()));
        acceptorConfig = ConfigUtils.inheritConfig(acceptorConfig, inheritedConfig.getAcceptorConfig());
        foragerConfig = ConfigUtils.inheritConfig(foragerConfig, inheritedConfig.getForagerConfig());
        return this;
    }

    @Override
    public @NonNull LocalSearchPhaseConfig copyConfig() {
        return new LocalSearchPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (terminationConfig != null) {
            terminationConfig.visitReferencedClasses(classVisitor);
        }
        if (moveSelectorConfig != null) {
            moveSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (moveProvidersClass != null) {
            classVisitor.accept(moveProvidersClass);
        }
        if (acceptorConfig != null) {
            acceptorConfig.visitReferencedClasses(classVisitor);
        }
        if (foragerConfig != null) {
            foragerConfig.visitReferencedClasses(classVisitor);
        }
    }

}
