package ai.timefold.solver.model.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ModelInfoBuildItem extends SimpleBuildItem {

    private final String modelId;
    private final String modelName;

    public ModelInfoBuildItem(String modelId, String modelName) {
        this.modelId = modelId;
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelId() {
        return modelId;
    }
}
