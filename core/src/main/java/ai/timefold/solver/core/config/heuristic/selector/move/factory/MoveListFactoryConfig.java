package ai.timefold.solver.core.config.heuristic.selector.move.factory;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbCustomPropertiesAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "moveListFactoryClass",
        "moveListFactoryCustomProperties"
})
public class MoveListFactoryConfig extends MoveSelectorConfig<MoveListFactoryConfig> {

    public static final String XML_ELEMENT_NAME = "moveListFactory";

    protected Class<? extends MoveListFactory> moveListFactoryClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> moveListFactoryCustomProperties = null;

    public @Nullable Class<? extends MoveListFactory> getMoveListFactoryClass() {
        return moveListFactoryClass;
    }

    public void setMoveListFactoryClass(@Nullable Class<? extends MoveListFactory> moveListFactoryClass) {
        this.moveListFactoryClass = moveListFactoryClass;
    }

    public @Nullable Map<String, String> getMoveListFactoryCustomProperties() {
        return moveListFactoryCustomProperties;
    }

    public void setMoveListFactoryCustomProperties(@Nullable Map<String, String> moveListFactoryCustomProperties) {
        this.moveListFactoryCustomProperties = moveListFactoryCustomProperties;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull MoveListFactoryConfig
            withMoveListFactoryClass(@NonNull Class<? extends MoveListFactory> moveListFactoryClass) {
        this.setMoveListFactoryClass(moveListFactoryClass);
        return this;
    }

    public @NonNull MoveListFactoryConfig
            withMoveListFactoryCustomProperties(@NonNull Map<String, String> moveListFactoryCustomProperties) {
        this.setMoveListFactoryCustomProperties(moveListFactoryCustomProperties);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull MoveListFactoryConfig inherit(@NonNull MoveListFactoryConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        moveListFactoryClass = ConfigUtils.inheritOverwritableProperty(
                moveListFactoryClass, inheritedConfig.getMoveListFactoryClass());
        moveListFactoryCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                moveListFactoryCustomProperties, inheritedConfig.getMoveListFactoryCustomProperties());
        return this;
    }

    @Override
    public @NonNull MoveListFactoryConfig copyConfig() {
        return new MoveListFactoryConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        classVisitor.accept(moveListFactoryClass);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + moveListFactoryClass + ")";
    }

}
