package ai.timefold.solver.jackson.api.score.buildin.simplelong;

import java.io.IOException;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class SimpleLongScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleLongScore> {

    @Override
    public SimpleLongScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return SimpleLongScore.parseScore(parser.getValueAsString());
    }

}
