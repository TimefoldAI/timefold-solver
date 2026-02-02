package ai.timefold.solver.jackson3.preview.api.domain.solution.diff;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class PlanningEntityDiffJacksonSerializer<Solution_, Entity_>
        extends ValueSerializer<PlanningEntityDiff<Solution_, Entity_>> {

    @Override
    public void serialize(PlanningEntityDiff<Solution_, Entity_> entityDiff, JsonGenerator jsonGenerator,
            SerializationContext serializerProvider) throws JacksonException {
        jsonGenerator.writePOJO(SerializablePlanningEntityDiff.of(entityDiff));
    }

}
