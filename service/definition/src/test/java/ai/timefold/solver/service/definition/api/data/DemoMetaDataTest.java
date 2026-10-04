package ai.timefold.solver.service.definition.api.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DemoMetaDataTest {

    @Test
    void constructorWithoutConfigUsesEmptyConfigList() {
        var metaData = new DemoMetaData("BASIC", "short", "long", List.of("tag"));

        assertThat(metaData.id()).isEqualTo("BASIC");
        assertThat(metaData.shortDescription()).isEqualTo("short");
        assertThat(metaData.longDescription()).isEqualTo("long");
        assertThat(metaData.tags()).containsExactly("tag");
        assertThat(metaData.config()).isEmpty();
    }

    @Test
    void constructorWithoutConfigMatchesExplicitEmptyConfig() {
        var withoutConfig = new DemoMetaData("BASIC", "short", "long", List.of("tag"));
        var withEmptyConfig = new DemoMetaData("BASIC", "short", "long", List.of("tag"), List.of());

        assertThat(withoutConfig).isEqualTo(withEmptyConfig);
    }
}
