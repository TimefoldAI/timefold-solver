package ai.timefold.solver.jackson.api.score.buildin;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class SimpleScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleScore> {

    @Override
    public SimpleScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return SimpleScore.parseScore(parser.getValueAsString());
    }

}
