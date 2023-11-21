package ai.timefold.solver.jackson.api.solver;

import java.io.IOException;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class RecommendedFitJacksonSerializer<Proposition_, Score_ extends Score<Score_>>
        extends JsonSerializer<RecommendedFit<Proposition_, Score_>> {
    @Override
    public void serialize(RecommendedFit<Proposition_, Score_> value, JsonGenerator gen, SerializerProvider serializerProvider)
            throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("proposition", value.result());
        gen.writeObjectField("scoreDiff", value.scoreAnalysisDiff());
        gen.writeEndObject();
    }
}
