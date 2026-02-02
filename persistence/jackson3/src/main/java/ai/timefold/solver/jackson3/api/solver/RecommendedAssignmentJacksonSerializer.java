package ai.timefold.solver.jackson3.api.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedAssignment;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class RecommendedAssignmentJacksonSerializer<Proposition_, Score_ extends Score<Score_>>
        extends ValueSerializer<RecommendedAssignment<Proposition_, Score_>> {
    @Override
    public void serialize(RecommendedAssignment<Proposition_, Score_> value, JsonGenerator gen,
            SerializationContext serializerProvider) throws JacksonException {
        gen.writeStartObject();
        gen.writePOJOProperty("proposition", value.proposition());
        gen.writePOJOProperty("scoreDiff", value.scoreAnalysisDiff());
        gen.writeEndObject();
    }
}
