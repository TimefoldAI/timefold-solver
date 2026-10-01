package ai.timefold.solver.service.definition.internal.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExecutionProfileRegistryTest {

    @Test
    void discoversRegisteredProfilesViaServiceLoader() {
        ExecutionProfileRegistry registry = new ExecutionProfileRegistry();
        // The seed profile is registered as a service, so it must be discovered.
        assertThat(registry.findById("seed")).isPresent();
        assertThat(registry.all()).extracting(ExecutionProfile::id).contains("seed");
    }

    @Test
    void findByIdReturnsEmptyForUnknownProfile() {
        ExecutionProfileRegistry registry = new ExecutionProfileRegistry();
        assertThat(registry.findById("does-not-exist")).isEmpty();
    }

    @Test
    void indexesProfilesById() {
        ExecutionProfile profile = new IdProfile("custom");
        ExecutionProfileRegistry registry = new ExecutionProfileRegistry(List.of(profile));
        assertThat(registry.findById("custom")).containsSame(profile);
        assertThat(registry.all()).containsExactly(profile);
    }

    private record IdProfile(String id) implements ExecutionProfile {
        @Override
        public String name() {
            return id;
        }

        @Override
        public String description() {
            return id;
        }
    }
}
