package ai.timefold.solver.service.definition.impl.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SeedExecutionProfileTest {

    private final SeedExecutionProfile profile = new SeedExecutionProfile();

    @Test
    void keepsSuppliedSeed() {
        Map<String, String> resolved = profile.resolveParameters(Map.of(SeedExecutionProfile.PARAMETER_SEED, "42"));
        assertThat(resolved).containsExactly(Map.entry(SeedExecutionProfile.PARAMETER_SEED, "42"));
        assertThat(profile.toEnvironment(resolved))
                .containsExactly(Map.entry(SeedExecutionProfile.ENV_QUARKUS_RANDOM_SEED, "42"));
    }

    @Test
    void generatesSeedWhenAbsent() {
        Map<String, String> resolved = profile.resolveParameters(Map.of());
        assertThat(resolved).containsOnlyKeys(SeedExecutionProfile.PARAMETER_SEED);
        // The generated seed is recorded and is a valid long.
        assertThatCode(() -> Long.parseLong(resolved.get(SeedExecutionProfile.PARAMETER_SEED)))
                .doesNotThrowAnyException();
        assertThat(profile.toEnvironment(resolved))
                .containsOnlyKeys(SeedExecutionProfile.ENV_QUARKUS_RANDOM_SEED);
    }

    @Test
    void ignoresUnrelatedOptions() {
        // The seed profile only reads its own option key; other run options are left alone.
        Map<String, String> resolved =
                profile.resolveParameters(Map.of("solver", "fast", SeedExecutionProfile.PARAMETER_SEED, "7"));
        assertThat(resolved).containsExactly(Map.entry(SeedExecutionProfile.PARAMETER_SEED, "7"));
    }

    @Test
    void rejectsNonLongSeed() {
        Map<String, String> options = Map.of(SeedExecutionProfile.PARAMETER_SEED, "not-a-number");
        assertThatThrownBy(() -> profile.resolveParameters(options))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emitsNoEnvironmentWithoutSeed() {
        assertThat(profile.toEnvironment(Map.of())).isEmpty();
    }
}
