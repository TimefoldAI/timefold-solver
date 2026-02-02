package ai.timefold.solver.jackson3.preview.api.domain.solution.diff;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class PlanningSolutionDiffJacksonSerializer<Solution_>
        extends ValueSerializer<PlanningSolutionDiff<Solution_>> {

    @Override
    public void serialize(PlanningSolutionDiff<Solution_> solutionDiff, JsonGenerator jsonGenerator,
            SerializationContext serializerProvider) throws JacksonException {
        jsonGenerator.writePOJO(SerializablePlanningSolutionDiff.of(solutionDiff));
    }

}
