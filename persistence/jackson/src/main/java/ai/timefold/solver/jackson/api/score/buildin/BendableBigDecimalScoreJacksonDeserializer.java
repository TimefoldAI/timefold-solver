package ai.timefold.solver.jackson.api.score.buildin;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class BendableBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<BendableBigDecimalScore> {

    @Override
    public BendableBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return BendableBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
