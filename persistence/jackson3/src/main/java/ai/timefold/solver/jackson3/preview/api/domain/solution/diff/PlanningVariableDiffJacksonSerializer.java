package ai.timefold.solver.jackson3.preview.api.domain.solution.diff;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningVariableDiff;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class PlanningVariableDiffJacksonSerializer<Solution_, Entity_, Value_>
        extends ValueSerializer<PlanningVariableDiff<Solution_, Entity_, Value_>> {

    @Override
    public void serialize(PlanningVariableDiff<Solution_, Entity_, Value_> variableDiff, JsonGenerator jsonGenerator,
            SerializationContext serializerProvider) throws JacksonException {
        jsonGenerator.writePOJO(SerializablePlanningVariableDiff.of(variableDiff));
    }

}
