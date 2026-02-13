package ai.timefold.solver.quarkus.jackson.score;

import java.io.IOException;

import ai.timefold.solver.core.api.score.SimpleScore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class SimpleScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleScore> {

    @Override
    public SimpleScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return SimpleScore.parseScore(parser.getValueAsString());
    }

}
