package ai.timefold.solver.service.definition.impl.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SeedExecutionProfileTest {

    private final SeedExecutionProfile profile = new SeedExecutionProfile();

    @Test
    void mapsSuppliedSeedToEnvironment() {
        assertThat(profile.toEnvironment(Map.of(SeedExecutionProfile.PARAMETER_SEED, "42")))
                .containsExactly(Map.entry(SeedExecutionProfile.ENV_QUARKUS_RANDOM_SEED, "42"));
    }

    @Test
    void ignoresUnrelatedOptions() {
        // The seed profile only reads its own option key; other run options are left alone.
        assertThat(profile.toEnvironment(Map.of("solver", "fast", SeedExecutionProfile.PARAMETER_SEED, "7")))
                .containsExactly(Map.entry(SeedExecutionProfile.ENV_QUARKUS_RANDOM_SEED, "7"));
    }

    @Test
    void emitsNoEnvironmentWhenSeedAbsent() {
        // No seed supplied: the profile contributes nothing (it never invents a seed).
        assertThat(profile.toEnvironment(Map.of())).isEmpty();
    }

    @Test
    void rejectsNonLongSeed() {
        Map<String, String> options = Map.of(SeedExecutionProfile.PARAMETER_SEED, "not-a-number");
        assertThatThrownBy(() -> profile.toEnvironment(options))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
