package ai.timefold.solver.service.definition.internal.descriptor;

/**
 * Describes a visualization page that a UI should render for a model.
 *
 * @param key stable identifier for the page
 * @param icon icon name for this page. Any icon name from <a href="https://tabler.io/icons">Tabler Icons</a> is valid.
 * @param label human-readable label for the page
 */
public record VisualizationPageDescriptor(String key, String icon, String label) {
}
