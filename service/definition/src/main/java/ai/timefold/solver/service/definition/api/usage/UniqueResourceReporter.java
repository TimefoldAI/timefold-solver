package ai.timefold.solver.service.definition.api.usage;

/**
 * Reports {@link UniqueResources} identified by the optimization model for usage metering.
 */
public interface UniqueResourceReporter {

    /**
     * Reports {@link UniqueResources} for a given operation.
     *
     * @param operationId ID of the operation to report resources for
     * @param uniqueResources {@link UniqueResources} used in the given operation
     */
    void reportUniqueResources(String operationId, UniqueResources uniqueResources);

    /**
     * Reports {@link UniqueResources} for a given operation.
     *
     * @param operationId ID of the operation to report resources for
     * @param datasetId ID of the dataset
     * @param uniqueResources {@link UniqueResources} used in the given operation
     */
    void reportUniqueResources(String operationId, String datasetId, UniqueResources uniqueResources);
}
