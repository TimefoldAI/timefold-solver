package ai.timefold.solver.service.definition.internal.executionprofile;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Discovers all {@link ExecutionProfile} implementations available on the classpath via {@link ServiceLoader} and exposes
 * them by name. Consumers use this registry rather than referencing individual profiles, so the set of profiles is
 * extensible without touching call sites.
 */
public final class ExecutionProfileRegistry {

    private final Map<String, ExecutionProfile> profilesByName;

    public ExecutionProfileRegistry() {
        this(ServiceLoader.load(ExecutionProfile.class));
    }

    ExecutionProfileRegistry(Iterable<ExecutionProfile> profiles) {
        this.profilesByName = StreamSupport.stream(profiles.spliterator(), false)
                .collect(Collectors.toUnmodifiableMap(ExecutionProfile::name, Function.identity()));
    }

    public Collection<ExecutionProfile> all() {
        return profilesByName.values();
    }

    public Optional<ExecutionProfile> findByName(String name) {
        return Optional.ofNullable(profilesByName.get(name));
    }
}
