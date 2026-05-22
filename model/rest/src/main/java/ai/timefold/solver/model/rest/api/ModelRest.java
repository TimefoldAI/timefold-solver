package ai.timefold.solver.model.rest.api;

import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;

/**
 * Interface used by models to indicate that REST API should be generated for given model.
 *
 * This interface will drive a complete REST API of the model and can also be used to extend the REST API with
 * additional endpoints by adding default interface methods.
 *
 * Models should only create interface extending this interface and not implement it as a class.
 */
public interface ModelRest {

    /**
     * Provides access to storage in case of extra endpoint methods needs to get hold of stored data like model input, output
     * etc
     *
     * @return configured storage service instance
     */
    AbstractStorageService storageService();
}
