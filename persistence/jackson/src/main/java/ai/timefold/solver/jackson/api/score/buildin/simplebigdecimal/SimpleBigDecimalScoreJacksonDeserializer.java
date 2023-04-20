package ai.timefold.solver.jackson.api.score.buildin.simplebigdecimal;

import java.io.IOException;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class SimpleBigDecimalScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleBigDecimalScore> {

    @Override
    public SimpleBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return SimpleBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
