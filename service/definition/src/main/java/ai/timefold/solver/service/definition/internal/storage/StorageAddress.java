package ai.timefold.solver.service.definition.internal.storage;

/**
 * Provides storage address used to store user model and its sub resources in the underlying data store
 */
public class StorageAddress {

    /**
     * Location definition within the data store, usually refers to bucket or folder
     */
    private final String location;

    /**
     * Model identifier that should be used to group user models of given model (including version),
     * usually represents a folder or prefix of the stored object
     */
    private final String modelId;

    private final String model;

    private final String modelVersion;

    public StorageAddress(String location, String model, String modelVersion) {
        this.location = location;
        this.modelId = modelVersion != null ? model + "/" + modelVersion : model;
        this.model = model;
        this.modelVersion = modelVersion;
    }

    public String getLocation() {
        return location;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModel() {
        return model;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    @Override
    public String toString() {
        return "(location=" + location + ", modelId=" + modelId + ")";
    }

}
