package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.validateApplicationVersion;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApplicationVersionValidationTest {

    @Test
    void validVersionDoesNotThrow() {
        assertThatCode(() -> validateApplicationVersion("v1")).doesNotThrowAnyException();
    }

    @Test
    void nullVersionThrows() {
        assertThatThrownBy(() -> validateApplicationVersion(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timefold.application.version");
    }

    @Test
    void emptyVersionThrows() {
        assertThatThrownBy(() -> validateApplicationVersion(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timefold.application.version");
    }

    @Test
    void blankVersionThrows() {
        assertThatThrownBy(() -> validateApplicationVersion("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timefold.application.version");
    }
}
