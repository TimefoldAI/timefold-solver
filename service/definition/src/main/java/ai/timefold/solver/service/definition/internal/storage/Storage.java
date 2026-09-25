package ai.timefold.solver.service.definition.internal.storage;

import java.io.InputStream;
import java.util.List;

import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;

/**
 * Storage responsible for persisting data sets and their sub models into a data store.
 * <p>
 * The storage deals with raw content only, it is completely unaware of the types it persists.
 * Turning objects into content and back, including compression and decompression, is the responsibility of
 * {@link AbstractStorageService}. Content is always exchanged as streams so that potentially large data sets
 * do not have to be kept in memory in their entirety.
 */
public interface Storage {

    String DATASETS_PREFIX = "datasets";

    String RUNS_PREFIX = "run";

    /**
     * Stores given data set into location defined by <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param content content of the data set to be stored
     */
    void store(StorageAddress address, String id, StorageContent content);

    /**
     * Updates existing data set in the storage in location defined by <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param content content of the data set to be stored
     */
    void update(StorageAddress address, String id, StorageContent content);

    /**
     * Final update of the data set upon completion of solving into location defined by <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param content content of the data set to be stored
     */
    void complete(StorageAddress address, String id, StorageContent content);

    /**
     * Retrieves content of the data set by its unique identifier from location defined by <code>StorageAddress</code>.
     * <p>
     * It is the responsibility of the caller to close the returned stream.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @return non null stream with the content of the data set
     * @throws ItemNotFoundException in case given data set does not exist
     */
    InputStream get(StorageAddress address, String id);

    /**
     * Deletes data set with given identifier, including all its sub models, from location defined by
     * <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     */
    void delete(StorageAddress address, String id);

    /**
     * Restores previously deleted data set with given identifier from location defined by <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @throws ItemNotFoundException in case restore cannot be performed
     */
    void restore(StorageAddress address, String id);

    /**
     * Checks if data set with given identifier exists in the location defined by <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @return true if exists false otherwise
     */
    boolean exists(StorageAddress address, String id);

    /**
     * Stores sub model associated with given data set in location defined by <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @param content content of the sub model to be stored
     */
    void storeSubModel(StorageAddress address, String id, SubModelKind kind, StorageContent content);

    /**
     * Updates already stored sub model associated with given data set in location defined by
     * <code>StorageAddress</code>.
     * <p>
     * Contrary to {@link #storeSubModel(StorageAddress, String, SubModelKind, StorageContent)}, implementations are
     * allowed to consider the update as a best effort operation, as updates are usually issued repeatedly while solving.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @param content content of the sub model to be stored
     */
    void updateSubModel(StorageAddress address, String id, SubModelKind kind, StorageContent content);

    /**
     * Retrieves content of the sub model associated with data set with given identifier from location defined by
     * <code>StorageAddress</code>.
     * <p>
     * It is the responsibility of the caller to close the returned stream.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @return stream with the content of the sub model or null if no such sub model is found
     */
    InputStream getSubModel(StorageAddress address, String id, SubModelKind kind);

    /**
     * Checks if sub model of given kind exists for the data set with given identifier in the location defined by
     * <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @return true if exists false otherwise
     */
    boolean existsSubModel(StorageAddress address, String id, SubModelKind kind);

    /**
     * Tells whether the underlying data store keeps the attributes given to it as part of
     * {@link StorageContent#attributes()}, so that they can be read back by
     * {@link #list(StorageAddress, int, int)} without reading the content itself.
     *
     * @return true if attributes are supported, which is the default
     */
    default boolean supportsAttributes() {
        return true;
    }

    /**
     * Lists data sets stored in the location defined by <code>StorageAddress</code>
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param pageNumber number of page to return (0-based)
     * @param pageSize number of data sets to return per page
     * @return non null list of found data sets
     */
    List<StorageItem> list(StorageAddress address, int pageNumber, int pageSize);

    /**
     * Cleans up the storage starting in the location defined by <code>StorageAddress</code>. Removes all resources
     * regardless of their type e.g. data sets, waypoints etc
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     */
    void clean(StorageAddress address);

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
}
