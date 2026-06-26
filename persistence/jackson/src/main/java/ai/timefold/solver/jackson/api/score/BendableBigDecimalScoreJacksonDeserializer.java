package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class BendableBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<BendableBigDecimalScore> {

    @Override
    public BendableBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        return BendableBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
