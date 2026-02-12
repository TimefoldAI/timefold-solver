package ai.timefold.solver.quarkus.jackson.score;

import java.io.IOException;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class BendableBigDecimalScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<BendableBigDecimalScore> {

    @Override
    public BendableBigDecimalScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return BendableBigDecimalScore.parseScore(parser.getValueAsString());
    }

}
