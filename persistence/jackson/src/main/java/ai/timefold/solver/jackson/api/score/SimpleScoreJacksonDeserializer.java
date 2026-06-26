package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.SimpleScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class SimpleScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleScore> {

    @Override
    public SimpleScore deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        return SimpleScore.parseScore(parser.getValueAsString());
    }

}
