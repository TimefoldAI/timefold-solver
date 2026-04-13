package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class SimpleBigDecimalScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleBigDecimalScore> {

    @Override
    public SimpleBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return SimpleBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
