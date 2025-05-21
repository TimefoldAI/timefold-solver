package ai.timefold.solver.core.impl.constructionheuristic.placer.internal;

import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class QueuedMultiplePlacerConfig extends EntityPlacerConfig<QueuedMultiplePlacerConfig> {

    protected List<EntityPlacerConfig> placerConfigList = null;

    public List<EntityPlacerConfig> getPlacerConfigList() {
        return placerConfigList;
    }

    public void setPlacerConfigList(List<EntityPlacerConfig> placerConfigList) {
        this.placerConfigList = placerConfigList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull QueuedMultiplePlacerConfig
            withPlacerConfigList(@NonNull List<@NonNull EntityPlacerConfig> placerConfigList) {
        setPlacerConfigList(placerConfigList);
        return this;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public @NonNull QueuedMultiplePlacerConfig
            inherit(@NonNull QueuedMultiplePlacerConfig inheritedConfig) {
        placerConfigList =
                ConfigUtils.inheritMergeableListConfig(placerConfigList, inheritedConfig.getPlacerConfigList());
        return this;
    }

    @Override
    public @NonNull QueuedMultiplePlacerConfig copyConfig() {
        return new QueuedMultiplePlacerConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<@Nullable Class<?>> classVisitor) {
        if (placerConfigList != null) {
            placerConfigList.forEach(placer -> placer.visitReferencedClasses(classVisitor));
        }
    }
}
