package ai.timefold.solver.jackson.api.score.buildin.bendable;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class BendableScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<BendableScore> {

    @Override
    public BendableScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return BendableScore.parseScore(parser.getValueAsString());
    }

}
