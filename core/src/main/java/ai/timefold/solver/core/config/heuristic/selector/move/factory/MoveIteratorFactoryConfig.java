package ai.timefold.solver.core.config.heuristic.selector.move.factory;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbCustomPropertiesAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "moveIteratorFactoryClass",
        "moveIteratorFactoryCustomProperties"
})
public class MoveIteratorFactoryConfig extends MoveSelectorConfig<MoveIteratorFactoryConfig> {

    public static final String XML_ELEMENT_NAME = "moveIteratorFactory";

    protected Class<? extends MoveIteratorFactory> moveIteratorFactoryClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> moveIteratorFactoryCustomProperties = null;

    public @Nullable Class<? extends MoveIteratorFactory> getMoveIteratorFactoryClass() {
        return moveIteratorFactoryClass;
    }

    public void setMoveIteratorFactoryClass(@Nullable Class<? extends MoveIteratorFactory> moveIteratorFactoryClass) {
        this.moveIteratorFactoryClass = moveIteratorFactoryClass;
    }

    public @Nullable Map<String, String> getMoveIteratorFactoryCustomProperties() {
        return moveIteratorFactoryCustomProperties;
    }

    public void setMoveIteratorFactoryCustomProperties(@Nullable Map<String, String> moveIteratorFactoryCustomProperties) {
        this.moveIteratorFactoryCustomProperties = moveIteratorFactoryCustomProperties;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull MoveIteratorFactoryConfig
            withMoveIteratorFactoryClass(@NonNull Class<? extends MoveIteratorFactory> moveIteratorFactoryClass) {
        this.setMoveIteratorFactoryClass(moveIteratorFactoryClass);
        return this;
    }

    public @NonNull MoveIteratorFactoryConfig
            withMoveIteratorFactoryCustomProperties(@NonNull Map<String, String> moveIteratorFactoryCustomProperties) {
        this.setMoveIteratorFactoryCustomProperties(moveIteratorFactoryCustomProperties);
        return this;
    }

    @Override
    public @NonNull MoveIteratorFactoryConfig inherit(@NonNull MoveIteratorFactoryConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        moveIteratorFactoryClass = ConfigUtils.inheritOverwritableProperty(
                moveIteratorFactoryClass, inheritedConfig.getMoveIteratorFactoryClass());
        moveIteratorFactoryCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                moveIteratorFactoryCustomProperties, inheritedConfig.getMoveIteratorFactoryCustomProperties());
        return this;
    }

    @Override
    public @NonNull MoveIteratorFactoryConfig copyConfig() {
        return new MoveIteratorFactoryConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        classVisitor.accept(moveIteratorFactoryClass);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + moveIteratorFactoryClass + ")";
    }

}
