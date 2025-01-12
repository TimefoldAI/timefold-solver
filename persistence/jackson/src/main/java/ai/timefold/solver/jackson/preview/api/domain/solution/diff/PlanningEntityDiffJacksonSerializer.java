package ai.timefold.solver.jackson.preview.api.domain.solution.diff;

import java.io.IOException;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class PlanningEntityDiffJacksonSerializer<Solution_, Entity_>
        extends JsonSerializer<PlanningEntityDiff<Solution_, Entity_>> {

    @Override
    public void serialize(PlanningEntityDiff<Solution_, Entity_> entityDiff, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(SerializablePlanningEntityDiff.of(entityDiff));
    }

}
