package ai.timefold.solver.jackson.api.score.buildin;

import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class HardMediumSoftBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardMediumSoftBigDecimalScore> {

    @Override
    public HardMediumSoftBigDecimalScore deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        return HardMediumSoftBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
