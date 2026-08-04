package ai.timefold.solver.service.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import ai.timefold.solver.service.definition.internal.descriptor.VisualizationPageDescriptor;
import ai.timefold.solver.service.quarkus.deployment.config.VisualizationPagesConfig;

import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class VisualizationPagesConfigTest {

    private static VisualizationPagesConfig buildConfig(Map<String, String> properties) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(VisualizationPagesConfig.class)
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .build();
        return config.getConfigMapping(VisualizationPagesConfig.class);
    }

    @Test
    void noPagesConfigured() {
        VisualizationPagesConfig config = buildConfig(Map.of());

        assertThat(TimefoldModelDescriptorProcessor.toVisualizationPageDescriptors(config)).isEmpty();
    }

    @Test
    void multiplePagesInOrder() {
        VisualizationPagesConfig config = buildConfig(Map.of(
                "timefold.model.visualization.pages[0].key", "data",
                "timefold.model.visualization.pages[0].icon", "TbDatabase",
                "timefold.model.visualization.pages[0].label", "Data",
                "timefold.model.visualization.pages[1].key", "visualization",
                "timefold.model.visualization.pages[1].icon", "TbEye",
                "timefold.model.visualization.pages[1].label", "Visualization"));

        assertThat(TimefoldModelDescriptorProcessor.toVisualizationPageDescriptors(config))
                .containsExactly(
                        new VisualizationPageDescriptor("data", "TbDatabase", "Data"),
                        new VisualizationPageDescriptor("visualization", "TbEye", "Visualization"));
    }

    @Test
    void missingFieldFailsToBind() {
        Map<String, String> properties = Map.of(
                "timefold.model.visualization.pages[0].key", "visualization",
                "timefold.model.visualization.pages[0].label", "Visualization");

        assertThatThrownBy(() -> buildConfig(properties))
                .hasMessageContaining("timefold.model.visualization.pages[0].icon");
    }
}
