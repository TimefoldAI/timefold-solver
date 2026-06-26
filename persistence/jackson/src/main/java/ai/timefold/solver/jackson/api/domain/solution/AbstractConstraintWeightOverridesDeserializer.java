package ai.timefold.solver.jackson.api.domain.solution;

import java.util.LinkedHashMap;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Extend this to implement {@link ConstraintWeightOverrides} deserialization specific for your domain.
 *
 * @param <Score_>
 */
public abstract class AbstractConstraintWeightOverridesDeserializer<Score_ extends Score<Score_>>
        extends JsonDeserializer<ConstraintWeightOverrides<Score_>> {

    @Override
    public final ConstraintWeightOverrides<Score_> deserialize(JsonParser p, DeserializationContext ctxt)
            throws java.io.IOException {
        var resultMap = new LinkedHashMap<String, Score_>();
        JsonNode node = p.readValueAsTree();
        node.fields().forEachRemaining(entry -> {
            var constraintId = entry.getKey();
            var weight = parseScore(entry.getValue().asText());
            resultMap.put(constraintId, weight);
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
