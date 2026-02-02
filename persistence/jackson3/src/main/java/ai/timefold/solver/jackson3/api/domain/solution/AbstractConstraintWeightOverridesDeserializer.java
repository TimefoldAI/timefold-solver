package ai.timefold.solver.jackson3.api.domain.solution;

import java.util.LinkedHashMap;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Extend this to implement {@link ConstraintWeightOverrides} deserialization specific for your domain.
 *
 * @param <Score_>
 */
public abstract class AbstractConstraintWeightOverridesDeserializer<Score_ extends Score<Score_>>
        extends ValueDeserializer<ConstraintWeightOverrides<Score_>> {

    @Override
    public final ConstraintWeightOverrides<Score_> deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException {
        var resultMap = new LinkedHashMap<String, Score_>();
        JsonNode node = p.readValueAsTree();
        node.properties().iterator().forEachRemaining(entry -> {
            var constraintName = entry.getKey();
            var weight = parseScore(entry.getValue().asString());
            resultMap.put(constraintName, weight);
        });
        return ConstraintWeightOverrides.of(resultMap);
    }

    /**
     * The domain is based on a single {@link Score} subtype.
     * This method is responsible for parsing the score string into that subtype.
     *
     * @param scoreString never null
     * @return never null
     */
    protected abstract Score_ parseScore(String scoreString);

}
