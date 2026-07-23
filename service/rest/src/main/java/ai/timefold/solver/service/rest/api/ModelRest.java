package ai.timefold.solver.service.rest.api;

/**
 * Interface used by models to indicate that REST API should be generated for given model.
 * <p>
 * This interface will drive a complete REST API of the model and can also be used to extend the REST API with
 * additional endpoints by adding default interface methods.
 * <p>
 * Models should only create interface extending this interface and not implement it as a class.
 */
public interface ModelRest {

}
