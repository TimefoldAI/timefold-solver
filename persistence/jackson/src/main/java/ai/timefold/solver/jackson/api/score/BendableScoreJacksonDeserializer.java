package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.BendableScore;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class BendableScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<BendableScore> {

    @Override
    public BendableScore deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        return BendableScore.parseScore(parser.getValueAsString());
    }

}
