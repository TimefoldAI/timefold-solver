package ai.timefold.solver.service.definition.api.usage;

/**
 * Implemented by optimization models that can identify their {@link UniqueResources}.
 */
public interface UniqueResourceAware {

    /**
     * Identifies the unique resources of the schedule.
     */
    UniqueResources getUniqueResources();
}
