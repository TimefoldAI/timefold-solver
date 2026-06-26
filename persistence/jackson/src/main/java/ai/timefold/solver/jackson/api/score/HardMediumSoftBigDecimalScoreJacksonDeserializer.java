package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class HardMediumSoftBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardMediumSoftBigDecimalScore> {

    @Override
    public HardMediumSoftBigDecimalScore deserialize(JsonParser parser, DeserializationContext context)
            throws java.io.IOException {
        return HardMediumSoftBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
