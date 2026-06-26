package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class HardSoftBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardSoftBigDecimalScore> {

    @Override
    public HardSoftBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        return HardSoftBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
