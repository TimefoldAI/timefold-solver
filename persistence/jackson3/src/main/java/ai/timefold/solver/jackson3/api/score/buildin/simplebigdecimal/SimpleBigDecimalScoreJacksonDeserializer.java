package ai.timefold.solver.jackson3.api.score.buildin.simplebigdecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class SimpleBigDecimalScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleBigDecimalScore> {

    @Override
    public SimpleBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return SimpleBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
