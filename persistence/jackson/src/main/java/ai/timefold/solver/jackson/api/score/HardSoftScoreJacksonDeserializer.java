package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardSoftScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class HardSoftScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<HardSoftScore> {

    @Override
    public HardSoftScore deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        return HardSoftScore.parseScore(parser.getValueAsString());
    }

}
