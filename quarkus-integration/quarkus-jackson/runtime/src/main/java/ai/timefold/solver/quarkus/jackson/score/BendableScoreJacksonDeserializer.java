package ai.timefold.solver.quarkus.jackson.score;

import java.io.IOException;

import ai.timefold.solver.core.api.score.BendableScore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class BendableScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<BendableScore> {

    @Override
    public BendableScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return BendableScore.parseScore(parser.getValueAsString());
    }

}
