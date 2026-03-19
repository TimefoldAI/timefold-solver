package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardSoftScore;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class HardSoftScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<HardSoftScore> {

    @Override
    public HardSoftScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return HardSoftScore.parseScore(parser.getValueAsString());
    }

}
