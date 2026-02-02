package ai.timefold.solver.jackson3.api.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * @deprecated Prefer to use the {@link RecommendedAssignmentJacksonSerializer} instead.
 */
@Deprecated(forRemoval = true, since = "1.15.0")
public final class RecommendedFitJacksonSerializer<Proposition_, Score_ extends Score<Score_>>
        extends ValueSerializer<RecommendedFit<Proposition_, Score_>> {
    @Override
    public void serialize(RecommendedFit<Proposition_, Score_> value, JsonGenerator gen,
            SerializationContext serializerProvider)
            throws JacksonException {
        gen.writeStartObject();
        gen.writePOJOProperty("proposition", value.proposition());
        gen.writePOJOProperty("scoreDiff", value.scoreAnalysisDiff());
        gen.writeEndObject();
    }
}
