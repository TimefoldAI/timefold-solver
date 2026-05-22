package ai.timefold.solver.model.definition.impl.storage.inmemory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import ai.timefold.solver.model.definition.api.ModelOutput;
import ai.timefold.solver.model.definition.api.domain.Metadata;
import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.definition.internal.storage.Storage;
import ai.timefold.solver.model.definition.internal.storage.StorageAddress;
import ai.timefold.solver.model.definition.internal.storage.StorageConfiguration;
import ai.timefold.solver.model.definition.internal.storage.SubModelKind;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class InMemoryStorage<ModelOutput_ extends ModelOutput> implements Storage<ModelOutput_> {

    private ObjectMapper mapper;

    private Map<String, ModelOutput_> models = new ConcurrentHashMap<>();

    private Map<String, Object> resources = new ConcurrentHashMap<>();

    public InMemoryStorage() {

    }

    public InMemoryStorage(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void store(StorageAddress options, String id, ModelOutput_ dataset) {
        models.put(id, dataset);
    }

    @Override
    public void update(StorageAddress options, String id, ModelOutput_ dataset) {
        models.put(id, dataset);
    }

    @Override
    public void complete(StorageAddress options, String id, ModelOutput_ dataset) {
        models.put(id, dataset);
    }

    @Override
    public ModelOutput_ get(StorageAddress options, String id) {
        return models.get(id);
    }

    @Override
    public void delete(StorageAddress options, String id) {
        for (SubModelKind submodel : SubModelKind.values()) {
            Object removed = resources.remove(id + "_" + submodel.id());

            if (removed != null) {
                resources.put(id + "_" + submodel.id() + ".deleted", removed);
            }
        }
        ModelOutput_ dataset = models.remove(id);
        if (dataset != null) {
            models.put(id + ".deleted", dataset);
        }
    }

    @Override
    public void restore(StorageAddress options, String id) {
        if (!models.containsKey(id + ".deleted")) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND,
                    "Run with id " + id + " cannot be restored as it does not exist");
        }
        ModelOutput_ dataset = models.remove(id + ".deleted");
        models.put(id, dataset);
        for (SubModelKind submodel : SubModelKind.values()) {
            Object restored = resources.remove(id + "_" + submodel.id() + ".deleted");

            if (restored != null) {
                resources.put(id + "_" + submodel.id(), restored);
            }
        }
    }

    @Override
    public boolean exists(StorageAddress options, String id) {
        return models.containsKey(id);
    }

    @Override
    public List<Metadata> list(StorageAddress options, int pageNumber, int pageSize) {
        return resources.values().stream().filter(item -> item instanceof Metadata)
                .skip((long) pageNumber * pageSize).limit(pageSize).map(Metadata.class::cast).toList();
    }

    @Override
    public <T> T getSubModel(StorageAddress options, String id, SubModelKind kind, Class<T> clazz) {

        return (T) resources.get(id + "_" + kind.id());
    }

    @Override
    public void storeSubModel(StorageAddress options, String id, SubModelKind kind, Object subModel) {
        if (subModel == null) {
            return;
        }
        resources.put(id + "_" + kind.id(), subModel);
    }

    @Override
    public void updateSubModel(StorageAddress options, String id, SubModelKind subModelKind, Object subModel) {
        if (subModel == null) {
            return;
        }
        resources.put(id + "_" + subModelKind.id(), subModel);
    }

    @Override
    public boolean existsSubModel(StorageAddress options, String id, SubModelKind kind) {
        return resources.containsKey(id + "_" + kind.id());
    }

    @Override
    public void getSubModelStream(StorageAddress options, String id, SubModelKind subModelKind, OutputStream output) {
        Object subModel = resources.get(id + "_" + subModelKind.id());
        if (subModel != null) {
            try {
                byte[] content = mapper.writeValueAsBytes(subModel);
                output.write(compress(content));

            } catch (IOException e) {
                throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_READ,
                        "Unable to read sub model (" + subModelKind + ") from the storage for id " + id, e);
            }
        }
    }

    @Override
    public void clean(StorageAddress options) {
        models.clear();
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

    @Override
    public <T> T getSubModel(StorageAddress options, String id, SubModelKind kind, TypeReference<T> configurationClass) {
        return (T) resources.get(id + "_" + kind.id());
    }

    private byte[] compress(byte[] data) {
        byte[] processed;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOs = new GZIPOutputStream(os)) {

            gzipOs.write(data, 0, data.length);

            gzipOs.close();

            processed = os.toByteArray();
        } catch (IOException e) {
            processed = data;
        }
        return processed;
    }
}
