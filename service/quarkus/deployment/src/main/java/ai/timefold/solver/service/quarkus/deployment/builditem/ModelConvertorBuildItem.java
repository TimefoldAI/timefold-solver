package ai.timefold.solver.service.quarkus.deployment.builditem;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ModelConvertorBuildItem extends SimpleBuildItem {

    private final DotName modelConvertorBean;

    public ModelConvertorBuildItem(DotName modelConvertorBean) {
        this.modelConvertorBean = modelConvertorBean;
    }

    public DotName getModelConvertorBean() {
        return modelConvertorBean;
    }
}
