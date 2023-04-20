package ai.timefold.solver.quarkus.jackson.deployment;

import ai.timefold.solver.quarkus.jackson.OptaPlannerObjectMapperCustomizer;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class OptaPlannerJacksonProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("optaplanner-jackson");
    }

    @BuildStep
    void registerOptaPlannerJacksonModule(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(OptaPlannerObjectMapperCustomizer.class));
    }

}
