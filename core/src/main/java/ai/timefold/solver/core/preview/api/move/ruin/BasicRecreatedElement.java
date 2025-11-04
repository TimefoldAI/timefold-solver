package ai.timefold.solver.core.preview.api.move.ruin;

public record BasicRecreatedElement<Entity_, Value_, Metadata_ extends RecreateMetadata>(
        Entity_ recreatedEntity,
        Metadata_ possibleValueMetadata) implements BasicRuinedOrRecreatedElement<Entity_, Value_, Metadata_> {
}
