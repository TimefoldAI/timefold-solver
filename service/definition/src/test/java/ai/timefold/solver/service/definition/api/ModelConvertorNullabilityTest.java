package ai.timefold.solver.service.definition.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

class ModelConvertorNullabilityTest {

    @Test
    void modelConvertorIsNullMarked() {
        assertThat(ModelConvertor.class.getAnnotation(NullMarked.class)).isNotNull();
    }
}
