package ai.timefold.solver.service.definition.internal.descriptor;

import ai.timefold.solver.service.definition.api.domain.DataFormat;

/**
 * Defines Output Metric of the model where information is retrieved from <code>ModelOutputMetrics</code> class of the model
 *
 * <ul>
 * <li>id - usually the field of the class representing given Output Metric</li>
 * <li>name - human readable name</li>
 * <li>description - human readable description of the Output Metrics</li>
 * <li>type - type of the Output Metric that provides information how to handle given Output Metric and what operations can be
 * performed on it e.g.
 * number, duration, distance_meters</li>
 * <li>priority - order in which Output Metrics should be presented</li>
 * <li>example - value that this Output Metrics can hold</li>
 * <li>exampleFormatted - example but formatted according to given type .e.g distance in meters can be presented in kilometers
 * for better readability</li>
 * </ul>
 */
public record OutputMetricDescriptor(String id, String name, String description, DataFormat type, Integer priority,
        String example,
        String exampleFormatted) {

}
