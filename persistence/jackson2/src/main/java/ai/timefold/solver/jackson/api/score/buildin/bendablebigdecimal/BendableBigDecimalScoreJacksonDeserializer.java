package ai.timefold.solver.jackson.api.score.buildin.bendablebigdecimal;

import java.io.IOException;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class BendableBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<BendableBigDecimalScore> {

    @Override
    public BendableBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return BendableBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
