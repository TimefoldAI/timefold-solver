package ai.timefold.solver.jackson.api.score.buildin.bendablelong;

import java.io.IOException;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class BendableLongScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<BendableLongScore> {

    @Override
    public BendableLongScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return BendableLongScore.parseScore(parser.getValueAsString());
    }

}
