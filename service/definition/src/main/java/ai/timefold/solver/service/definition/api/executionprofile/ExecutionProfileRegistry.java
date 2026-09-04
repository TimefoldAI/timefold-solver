package ai.timefold.solver.service.definition.api.executionprofile;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Discovers all {@link ExecutionProfile} implementations available on the classpath (as CDI beans) and exposes them by name.
 * Consumers inject this registry rather than referencing individual profiles, so the set of profiles is extensible without
 * touching call sites.
 */
@ApplicationScoped
public class ExecutionProfileRegistry {

    private final Map<String, ExecutionProfile> profilesByName;

    @Inject
    public ExecutionProfileRegistry(Instance<ExecutionProfile> profiles) {
        this.profilesByName = profiles.stream()
                .collect(Collectors.toUnmodifiableMap(ExecutionProfile::name, Function.identity()));
    }

    public Collection<ExecutionProfile> all() {
        return profilesByName.values();
    }

    public Optional<ExecutionProfile> findByName(String name) {
        return Optional.ofNullable(profilesByName.get(name));
    }
}
