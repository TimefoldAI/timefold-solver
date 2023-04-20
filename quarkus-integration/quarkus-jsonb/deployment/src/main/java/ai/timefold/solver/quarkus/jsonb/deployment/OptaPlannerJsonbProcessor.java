package ai.timefold.solver.quarkus.jsonb.deployment;

import ai.timefold.solver.quarkus.jsonb.OptaPlannerJsonbConfigCustomizer;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class OptaPlannerJsonbProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("optaplanner-jsonb");
    }

    @BuildStep
    void registerOptaPlannerJsonbConfig(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(OptaPlannerJsonbConfigCustomizer.class));
    }

}
