package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class HardMediumSoftScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardMediumSoftScore> {

    @Override
    public HardMediumSoftScore deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        return HardMediumSoftScore.parseScore(parser.getValueAsString());
    }

}
