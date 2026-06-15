package ai.timefold.solver.service.definition.internal.storage;

import java.io.OutputStream;
import java.util.List;

import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Storage responsible for persisting <code>ModelOutput_</code> and its sub resources into a data store
 *
 * @param <ModelOutput_> representing a solved data set
 */
public interface Storage<ModelOutput_> {

    public static final String DATASETS_PREFIX = "datasets";

    public static final String RUNS_PREFIX = "run";

    /**
     * Stores given data set into default location in the underlying data store
     *
     * @param id unique identifier of the data set
     * @param dataset data set to be stored
     */
    default void store(String id, ModelOutput_ dataset) {
        store(null, id, dataset);
    }

    /**
     * Stores given data set into location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param dataset data set to be stored
     */
    void store(StorageAddress options, String id, ModelOutput_ dataset);

    /**
     * Updates existing data set in the storage under default location in the underlying data store
     *
     * @param id unique identifier of the data set
     * @param dataset data set to be stored
     */
    default void update(String id, ModelOutput_ dataset) {
        update(null, id, dataset);
    }

    /**
     * Updates existing data set in the storage into location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param dataset data set to be stored
     */
    void update(StorageAddress options, String id, ModelOutput_ dataset);

    /**
     * Final update of the data set upon completion of solving into default location in the underlying data store
     *
     * @param id unique identifier of the data set
     * @param dataset data set to be stored
     */
    default void complete(String id, ModelOutput_ dataset) {
        complete(null, id, dataset);
    }

    /**
     * Final update of the data set upon completion of solving into location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param dataset data set to be stored
     */
    void complete(StorageAddress options, String id, ModelOutput_ dataset);

    /**
     * Retrieves data set by its unique identifier from the default location in the underlying data store
     *
     * @param id unique identifier of the data set
     * @return loaded data set if found
     * @throws ItemNotFoundException in case given data set does not exist
     */
    default ModelOutput_ get(String id) {
        return get(null, id);
    }

    /**
     * Retrieves data set by its unique identifier from location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @return loaded data set if found
     * @throws ItemNotFoundException in case given data set does not exist
     */
    ModelOutput_ get(StorageAddress options, String id);

    /**
     * Deletes data set with given identifier from the default location in the underlying data store
     *
     * @param id unique identifier of the data set
     */
    default void delete(String id) {
        delete(null, id);
    }

    /**
     * Deletes data set with given identifier from location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     */
    void delete(StorageAddress options, String id);

    /**
     * Restores previously deleted set with given identifier from the default location in the underlying data store
     *
     * @param id unique identifier of the data set
     * @throws ItemNotFoundException in case restore cannot be performed
     */
    default void restore(String id) {
        restore(null, id);
    }

    /**
     * Restores previously deleted data set with given identifier from location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @throws ItemNotFoundException in case restore cannot be performed
     */
    void restore(StorageAddress options, String id);

    /**
     * Checks if data set with given identifier exists in the default location in the underlying data store
     *
     * @param id unique identifier of the data set
     * @return true if exists false otherwise
     */
    default boolean exists(String id) {
        return exists(null, id);
    }

    /**
     * Checks if data set with given identifier exists in the location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param kind type of sub model
     * @return true if exists false otherwise
     */
    boolean existsSubModel(StorageAddress options, String id, SubModelKind kind);

    /**
     * Checks if data set with given identifier exists in the default location in the underlying data store
     *
     * @param id unique identifier of the data set
     * @param kind type of sub model
     * @return true if exists false otherwise
     */
    default boolean existsSubModel(String id, SubModelKind kind) {
        return existsSubModel(null, id, kind);
    }

    /**
     * Checks if data set with given identifier exists in the location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @return true if exists false otherwise
     */
    boolean exists(StorageAddress options, String id);

    /**
     * Lists data sets (as statuses) stored in the default location of the underlying data store
     *
     * @param pageNumber number of page to return (0-based)
     * @param pageSize number of data sets to return per page
     * @return non null list of found data sets as statuses
     */
    default <Score_> List<Metadata<Score_>> list(int pageNumber, int pageSize) {
        return list(null, pageNumber, pageSize);
    }

    /**
     * Lists data sets (as statuses) stored in the location defined by <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param pageNumber number of page to return (0-based)
     * @param pageSize number of data sets to return per page
     * @return non null list of found data sets as statuses
     */
    <Score_> List<Metadata<Score_>> list(StorageAddress options, int pageNumber, int pageSize);

    /**
     * Retrieves sub resource associated with data set with given identifier in the default location in the underlying data
     * store as a stream of data
     *
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param output output stream where data should be written
     */
    default void getSubModelStream(String id, SubModelKind subModelKind, OutputStream output) {
        getSubModelStream(null, id, subModelKind, output);
    }

    /**
     * Retrieves sub resource associated with data set with given identifier in the default location in the underlying data
     * store as a stream of data
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param output output stream where data should be written
     */
    void getSubModelStream(StorageAddress options, String id, SubModelKind subModelKind, OutputStream output);

    /**
     * Retrieves sub resource associated with data set with given identifier in the default location in the underlying data
     * store
     *
     * @param <T> type representing the returned value
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param clazz class that the sub resource should be unmarshalled to
     * @return loaded sub resource or null if no sub resource found
     */
    default <T> T getSubModel(String id, SubModelKind subModelKind, Class<T> clazz) {
        return getSubModel(null, id, subModelKind, clazz);
    }

    /**
     * Retrieves sub resource associated with data set with given identifier in the location defined by
     * <code>StorageOptions</code>
     *
     * @param <T> type representing the returned value
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param clazz class that the sub resource should be unmarshalled
     * @return loaded sub resource or null if no sub resource found
     */
    <T> T getSubModel(StorageAddress options, String id, SubModelKind subModelKind, Class<T> clazz);

    default <T> T getSubModel(String id, SubModelKind config, TypeReference<T> configurationClass) {
        return getSubModel(null, id, config, configurationClass);
    }

    <T> T getSubModel(StorageAddress options, String id, SubModelKind config, TypeReference<T> configurationClass);

    /**
     * Stores sub resource associated with given data set given by identifier in the default location of the underlying data
     * store
     *
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param subModel the sub resource to be stored
     */
    default void storeSubModel(String id, SubModelKind subModelKind, Object subModel) {
        storeSubModel(null, id, subModelKind, subModel);
    }

    /**
     * Stores sub resource associated with given data set given by identifier in the location defined by
     * <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param subModel the sub resource to be stored
     */
    void storeSubModel(StorageAddress options, String id, SubModelKind subModelKind, Object subModel);

    /**
     * Stores sub resource associated with given data set given by identifier in the default location of the underlying data
     * store
     *
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param subModel the sub resource to be stored
     */
    default void updateSubModel(String id, SubModelKind subModelKind, Object subModel) {
        updateSubModel(null, id, subModelKind, subModel);
    }

    /**
     * Stores sub resource associated with given data set given by identifier in the location defined by
     * <code>StorageOptions</code>
     *
     * @param options storage option to apply during the operation
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub resource e.g. waypoints
     * @param subModel the sub resource to be stored
     */
    void updateSubModel(StorageAddress options, String id, SubModelKind subModelKind, Object subModel);

    /**
     * Cleans up the storage starting in the default location in the underlying data store. Removes all resources regardless of
     * their type e.g. data sets, waypoints etc
     */
    default void clean() {
        clean(null);
    }

    /**
     * Cleans up the storage starting in the location defined by
     * <code>StorageOptions</code>. Removes all resources regardless of
     * their type e.g. data sets, waypoints etc
     *
     * @param options storage option to apply during the operation
     */
    void clean(StorageAddress options);

    /**
     * Create required data store specific settings to be able to storage data in given location.
     *
     * @param location named location to be created in the underlying storage e.g. name of the bucket
     * @param configuration storage specific configuration
     */
    void create(String location, StorageConfiguration configuration);

    /**
     * Reconfigures already existing storage based on provided configuration
     *
     * @param location named location to be reconfigured in the underlying storage e.g. name of the bucket
     * @param configuration storage specific configuration
     */
    void reconfigure(String location, StorageConfiguration configuration);

    /**
     * Destroy data store specific settings, this will also remove all data stored under given location
     *
     * @param id named location to be destroyed in the underlying storage e.g. name of the bucket
     */
    void destroy(String id);

    Class<ModelOutput_> clazz();
}
