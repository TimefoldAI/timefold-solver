package ai.timefold.solver.quarkus.jackson.deployment;

import ai.timefold.solver.jackson.api.TimefoldJacksonModule;
import ai.timefold.solver.quarkus.jackson.TimefoldObjectMapperCustomizer;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;

class TimefoldJacksonProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("timefold-solver-jackson");
    }

    @BuildStep
    ClassPathJacksonModuleBuildItem registerTimefoldJacksonModule() {
        // Make timefold-solver-jackson discoverable by quarkus-rest
        // https://quarkus.io/guides/rest-migration#service-loading
        return new ClassPathJacksonModuleBuildItem(TimefoldJacksonModule.class.getName());
    }

    @BuildStep
    void registerTimefoldObjectMapperCustomizer(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(TimefoldObjectMapperCustomizer.class));
    }

}
