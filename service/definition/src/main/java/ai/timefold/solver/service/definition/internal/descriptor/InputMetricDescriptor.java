package ai.timefold.solver.service.definition.internal.descriptor;

import ai.timefold.solver.service.definition.api.domain.DataFormat;

/**
 * Defines InputMetric of the model where information is retrieved from <code>ModelInputMetric</code> class of the model
 *
 * <ul>
 * <li>id - usually the field of the class representing given InputMetric</li>
 * <li>name - human readable name</li>
 * <li>description - human readable description of the InputMetric</li>
 * <li>type - type of the InputMetric that provides information how to handle given InputMetric and what operations can be
 * performed on it e.g.
 * number, duration, distance_meters</li>
 * <li>priority - order in which InputMetric should be presented</li>
 * <li>example - value that this InputMetric can hold</li>
 * <li>exampleFormatted - example but formatted according to given type .e.g distance in meters can be presented in kilometers
 * for better readability</li>
 * </ul>
 */
public record InputMetricDescriptor(String id, String name, String description, DataFormat type, Integer priority,
        String example,
        String exampleFormatted) {

}
