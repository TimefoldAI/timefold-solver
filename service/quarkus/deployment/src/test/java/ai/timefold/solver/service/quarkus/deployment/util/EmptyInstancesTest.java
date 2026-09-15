package ai.timefold.solver.service.quarkus.deployment.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmptyInstancesTest {

    @Test
    void instantiatesClassViaNoArgConstructor() throws Exception {
        PlainConfig instance = EmptyInstances.of(PlainConfig.class);

        assertThat(instance).isNotNull();
        assertThat(instance.value).isNull();
    }

    @Test
    void instantiatesRecordViaCanonicalConstructorWithNullComponents() throws Exception {
        RecordConfig instance = EmptyInstances.of(RecordConfig.class);

        assertThat(instance).isNotNull();
        assertThat(instance.name()).isNull();
        assertThat(instance.weight()).isNull();
    }

    @Test
    void instantiatesRecordWithPrimitiveDefaults() throws Exception {
        PrimitiveRecordConfig instance = EmptyInstances.of(PrimitiveRecordConfig.class);

        assertThat(instance.enabled()).isFalse();
        assertThat(instance.count()).isZero();
        assertThat(instance.ratio()).isZero();
        assertThat(instance.label()).isNull();
    }

    @Test
    void compactConstructorStillRunsForEmptyRecord() throws Exception {
        CompactRecordConfig instance = EmptyInstances.of(CompactRecordConfig.class);

        assertThat(instance.name()).isEqualTo("default");
    }

    @Test
    void failsForClassWithoutNoArgConstructor() {
        assertThatThrownBy(() -> EmptyInstances.of(ClassWithoutNoArgConstructor.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    public static class PlainConfig {
        public String value;
    }

    public record RecordConfig(String name, Integer weight) {
    }

    public record PrimitiveRecordConfig(boolean enabled, int count, double ratio, String label) {
    }

    public record CompactRecordConfig(String name) {
        public CompactRecordConfig {
            if (name == null) {
                name = "default";
            }
        }
    }

    public static class ClassWithoutNoArgConstructor {
        public ClassWithoutNoArgConstructor(String value) {
        }
    }
}
