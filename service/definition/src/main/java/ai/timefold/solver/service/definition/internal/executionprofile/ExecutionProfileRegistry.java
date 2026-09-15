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
 * them by their stable {@link ExecutionProfile#id() id}. Consumers use this registry rather than referencing individual
 * profiles, so the set of profiles is extensible without touching call sites.
 */
public final class ExecutionProfileRegistry {

    private final Map<String, ExecutionProfile> profilesById;

    public ExecutionProfileRegistry() {
        this(ServiceLoader.load(ExecutionProfile.class));
    }

    ExecutionProfileRegistry(Iterable<ExecutionProfile> profiles) {
        this.profilesById = StreamSupport.stream(profiles.spliterator(), false)
                .collect(Collectors.toUnmodifiableMap(ExecutionProfile::id, Function.identity()));
    }

    public Collection<ExecutionProfile> all() {
        return profilesById.values();
    }

    public Optional<ExecutionProfile> findById(String id) {
        return Optional.ofNullable(profilesById.get(id));
    }
}
