package ai.timefold.solver.quarkus.jackson.deployment;

import ai.timefold.solver.quarkus.jackson.TimefoldObjectMapperCustomizer;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class TimefoldJacksonProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("timefold-solver-jackson");
    }

    @BuildStep
    void registerTimefoldJacksonModule(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(TimefoldObjectMapperCustomizer.class));
    }

}
