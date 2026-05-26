package ai.timefold.solver.model.definition.impl.storage;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.model.definition.internal.storage.StorageAddress;

public class StorageUtils {

    public static final String EMPTY_PREFIX = "   ";// empty prefix used in config properties cannot be an empty string as it will be ignored

    public static String bucket(String prefix, StorageAddress options, String defaultValue) {
        String bucketName;
        if (options != null && options.getLocation() != null && !options.getLocation().trim().isEmpty()) {
            bucketName = options.getLocation();
        } else {
            bucketName = defaultValue;
        }

        if (prefix != null) {
            return prefix + bucketName;
        } else {
            return bucketName;
        }

    }

    public static String id(StorageAddress options, String... prefixes) {
        String id = Stream.of(prefixes).filter(prefix -> prefix != null && !prefix.trim().isEmpty())
                .collect(Collectors.joining("/"));
        if (options != null && options.getModelId() != null && !options.getModelId().trim().isEmpty()) {
            return options.getModelId() + "/" + id;
        }

        return id;
    }
}
