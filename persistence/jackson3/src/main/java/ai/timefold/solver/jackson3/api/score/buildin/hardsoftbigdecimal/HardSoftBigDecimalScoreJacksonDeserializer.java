package ai.timefold.solver.jackson3.api.score.buildin.hardsoftbigdecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonDeserializer;

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
