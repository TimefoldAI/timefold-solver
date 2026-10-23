package ai.timefold.solver.service.definition.api.usage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Contains IDs of unique resources identified by the optimization model.
 */
public record UniqueResources(Map<String, Collection<String>> uniqueResourceIdsPerType) {

    /**
     * Constructs the {@link UniqueResources} from a single resource type.
     */
    public static UniqueResources from(String resourceType, Collection<String> uniqueResourceIds) {
        return new UniqueResources(Map.of(resourceType, uniqueResourceIds));
    }

    /**
     * Creates a new builder to register unique resources for multiple resource types.
     */
    public static UniqueResourcesBuilder builder() {
        return new UniqueResourcesBuilder();
    }

    public UniqueResources {
        Objects.requireNonNull(uniqueResourceIdsPerType);
        Map<String, Collection<String>> copy = new LinkedHashMap<>();
        // Defensive deep copy to maintain the original order, uniqueness and avoid random changes.
        for (var originalEntry : uniqueResourceIdsPerType.entrySet()) {
            Objects.requireNonNull(originalEntry.getKey());
            Objects.requireNonNull(originalEntry.getValue());
            copy.put(originalEntry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(originalEntry.getValue())));
        }
        uniqueResourceIdsPerType = Collections.unmodifiableMap(copy);
    }

    /**
     * Simplifies collecting the unique resource IDs for models with multiple resource types.
     */
    public static final class UniqueResourcesBuilder {
        private final Map<String, Collection<String>> uniqueResourceIdsPerType = new LinkedHashMap<>();

        /**
         * Adds unique resource IDs for the given resource types.
         * In case the resource type already exists in the builder, the unique resource IDs are merged.
         * 
         * @param resourceType identifier of the resource type
         * @param uniqueResourceIds collection of unique resource IDs of the given type
         */
        public UniqueResourcesBuilder addResourceType(String resourceType, Collection<String> uniqueResourceIds) {
            Objects.requireNonNull(resourceType);
            Objects.requireNonNull(uniqueResourceIds);
            uniqueResourceIdsPerType.computeIfAbsent(resourceType, unused -> new LinkedHashSet<>()).addAll(uniqueResourceIds);
            return this;
        }

        public UniqueResources build() {
            return new UniqueResources(uniqueResourceIdsPerType);
        }
    }
}
