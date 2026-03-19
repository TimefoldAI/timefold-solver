package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class HardSoftBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardSoftBigDecimalScore> {

    @Override
    public HardSoftBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return HardSoftBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
