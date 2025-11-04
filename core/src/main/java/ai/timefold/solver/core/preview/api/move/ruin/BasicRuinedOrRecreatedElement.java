package ai.timefold.solver.core.preview.api.move.ruin;

public sealed interface BasicRuinedOrRecreatedElement<Entity_, Value_, Metadata_ extends RecreateMetadata>
        permits BasicRecreatedElement, BasicRuinedElement {
}
