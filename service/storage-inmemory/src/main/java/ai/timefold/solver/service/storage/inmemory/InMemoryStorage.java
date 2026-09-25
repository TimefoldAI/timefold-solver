package ai.timefold.solver.service.storage.inmemory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.storage.Storage;
import ai.timefold.solver.service.definition.internal.storage.StorageAddress;
import ai.timefold.solver.service.definition.internal.storage.StorageConfiguration;
import ai.timefold.solver.service.definition.internal.storage.StorageContent;
import ai.timefold.solver.service.definition.internal.storage.StorageItem;
import ai.timefold.solver.service.definition.internal.storage.SubModelKind;
import ai.timefold.solver.service.definition.internal.storage.SupportedStorages;

import io.quarkus.arc.lookup.LookupIfProperty;

/**
 * Storage that keeps the content in memory, intended for development and testing purposes.
 * <p>
 * The content is kept as it was handed over by the storage service, meaning compressed, so that this storage behaves
 * the same way as the ones backed by an actual data store.
 */
@ApplicationScoped
@LookupIfProperty(name = SupportedStorages.STORAGE_TYPE_PROPERTY, stringValue = SupportedStorages.INMEMORY_STORAGE,
        lookupIfMissing = true)
public class InMemoryStorage implements Storage {

    private static final String DELETED_SUFFIX = ".deleted";

    private final Map<String, byte[]> datasets = new ConcurrentHashMap<>();

    private final Map<String, byte[]> subModels = new ConcurrentHashMap<>();

    private final Map<String, Map<String, String>> subModelAttributes = new ConcurrentHashMap<>();

    @Override
    public void store(StorageAddress address, String id, StorageContent content) {
        datasets.put(id, readAllBytes(content));
    }

    @Override
    public void update(StorageAddress address, String id, StorageContent content) {
        datasets.put(id, readAllBytes(content));
    }

    @Override
    public void complete(StorageAddress address, String id, StorageContent content) {
        datasets.put(id, readAllBytes(content));
    }

    @Override
    public InputStream get(StorageAddress address, String id) {
        byte[] content = datasets.get(id);
        if (content == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Unable to find dataset for id " + id);
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public void delete(StorageAddress address, String id) {
        for (SubModelKind kind : SubModelKind.values()) {
            byte[] removed = subModels.remove(key(id, kind));
            Map<String, String> removedAttributes = subModelAttributes.remove(key(id, kind));

            if (removed != null) {
                subModels.put(key(id, kind) + DELETED_SUFFIX, removed);
                if (removedAttributes != null) {
                    subModelAttributes.put(key(id, kind) + DELETED_SUFFIX, removedAttributes);
                }
            }
        }
        byte[] dataset = datasets.remove(id);
        if (dataset != null) {
            datasets.put(id + DELETED_SUFFIX, dataset);
        }
    }

    @Override
    public void restore(StorageAddress address, String id) {
        if (!datasets.containsKey(id + DELETED_SUFFIX)) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND,
                    "Run with id " + id + " cannot be restored as it does not exist");
        }
        datasets.put(id, datasets.remove(id + DELETED_SUFFIX));
        for (SubModelKind kind : SubModelKind.values()) {
            byte[] restored = subModels.remove(key(id, kind) + DELETED_SUFFIX);
            Map<String, String> restoredAttributes = subModelAttributes.remove(key(id, kind) + DELETED_SUFFIX);

            if (restored != null) {
                subModels.put(key(id, kind), restored);
                if (restoredAttributes != null) {
                    subModelAttributes.put(key(id, kind), restoredAttributes);
                }
            }
        }
    }

    @Override
    public boolean exists(StorageAddress address, String id) {
        return datasets.containsKey(id);
    }

    @Override
    public void storeSubModel(StorageAddress address, String id, SubModelKind kind, StorageContent content) {
        subModels.put(key(id, kind), readAllBytes(content));
        subModelAttributes.put(key(id, kind), content.attributes());
    }

    @Override
    public void updateSubModel(StorageAddress address, String id, SubModelKind kind, StorageContent content) {
        storeSubModel(address, id, kind, content);
    }

    @Override
    public InputStream getSubModel(StorageAddress address, String id, SubModelKind kind) {
        byte[] content = subModels.get(key(id, kind));

        return content == null ? null : new ByteArrayInputStream(content);
    }

    @Override
    public boolean existsSubModel(StorageAddress address, String id, SubModelKind kind) {
        return subModels.containsKey(key(id, kind));
    }

    @Override
    public List<StorageItem> list(StorageAddress address, int pageNumber, int pageSize) {
        String suffix = "_" + SubModelKind.METADATA.id();
        List<StorageItem> items = new ArrayList<>();
        for (String key : subModels.keySet()) {
            if (key.endsWith(suffix)) {
                String id = key.substring(0, key.length() - suffix.length());
                items.add(new StorageItem(id, subModelAttributes.getOrDefault(key, Map.of())));
            }
        }
        return items.stream().skip((long) pageNumber * pageSize).limit(pageSize).toList();
    }

    @Override
    public void clean(StorageAddress address) {
        datasets.clear();
        subModels.clear();
        subModelAttributes.clear();
    }

    @Override
    public void create(String location, StorageConfiguration configuration) {
    }

    @Override
    public void reconfigure(String location, StorageConfiguration configuration) {

    }

    @Override
    public void destroy(String id) {

    }

    private static String key(String id, SubModelKind kind) {
        return id + "_" + kind.id();
    }

    private static byte[] readAllBytes(StorageContent content) {
        try (InputStream stream = content.stream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the content to be stored", e);
        }
    }
}
