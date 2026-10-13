package ai.timefold.solver.service.definition.internal.storage;

import java.util.Map;
import java.util.Objects;

/**
 * A data set found in a {@link Storage}.
 *
 * @param id unique identifier of the data set, without any storage specific prefixes
 * @param attributes attributes associated with the data set in the underlying data store, never null but possibly
 *        empty when the data store does not support them
 */
public record StorageItem(String id, Map<String, String> attributes) {

    public StorageItem {
        Objects.requireNonNull(id, "id cannot be null");
        if (attributes == null) {
            attributes = Map.of();
        }
    }

    public StorageItem(String id) {
        this(id, Map.of());
    }
}
