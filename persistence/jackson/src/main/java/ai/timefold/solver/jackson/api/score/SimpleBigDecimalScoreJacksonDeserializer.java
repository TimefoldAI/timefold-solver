package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class SimpleBigDecimalScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleBigDecimalScore> {

    @Override
    public SimpleBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        return SimpleBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
