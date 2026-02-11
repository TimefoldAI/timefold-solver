package ai.timefold.solver.quarkus.jackson.diff;

import java.io.IOException;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class PlanningSolutionDiffJacksonSerializer<Solution_>
        extends JsonSerializer<PlanningSolutionDiff<Solution_>> {

    @Override
    public void serialize(PlanningSolutionDiff<Solution_> solutionDiff, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(SerializablePlanningSolutionDiff.of(solutionDiff));
    }

}
