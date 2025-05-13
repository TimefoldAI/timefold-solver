package ai.timefold.solver.core.config.constructionheuristic.placer;

import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "placerConfigList"
})
public class CartesianProductQueuedMultipleEntityValuePlacerConfig
        extends AbstractMultipleEntityValuePlacerConfig<CartesianProductQueuedMultipleEntityValuePlacerConfig> {

    @XmlElements({
            @XmlElement(name = QueuedEntityPlacerConfig.XML_ELEMENT_NAME,
                    type = QueuedEntityPlacerConfig.class),
            @XmlElement(name = QueuedValuePlacerConfig.XML_ELEMENT_NAME, type = QueuedValuePlacerConfig.class),
    })
    protected List<EntityPlacerConfig> placerConfigList = null;

    @Override
    public List<EntityPlacerConfig> getPlacerConfigList() {
        return placerConfigList;
    }

    public void setPlacerConfigList(List<EntityPlacerConfig> placerConfigList) {
        this.placerConfigList = placerConfigList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull CartesianProductQueuedMultipleEntityValuePlacerConfig
            withPlacerConfigList(@NonNull List<@NonNull EntityPlacerConfig> placerConfigList) {
        setPlacerConfigList(placerConfigList);
        return this;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public @NonNull CartesianProductQueuedMultipleEntityValuePlacerConfig
            inherit(@NonNull CartesianProductQueuedMultipleEntityValuePlacerConfig inheritedConfig) {
        placerConfigList =
                ConfigUtils.inheritMergeableListConfig(placerConfigList, inheritedConfig.getPlacerConfigList());
        return this;
    }

    @Override
    public @NonNull CartesianProductQueuedMultipleEntityValuePlacerConfig copyConfig() {
        return new CartesianProductQueuedMultipleEntityValuePlacerConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<@Nullable Class<?>> classVisitor) {
        if (placerConfigList != null) {
            placerConfigList.forEach(placer -> placer.visitReferencedClasses(classVisitor));
        }
    }
}
