package ai.timefold.solver.quarkus.jackson.score;

import java.io.IOException;

import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class HardMediumSoftBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardMediumSoftBigDecimalScore> {

    @Override
    public HardMediumSoftBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return HardMediumSoftBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
