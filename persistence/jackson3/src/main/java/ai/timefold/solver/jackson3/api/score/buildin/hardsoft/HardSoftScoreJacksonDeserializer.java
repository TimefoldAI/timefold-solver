package ai.timefold.solver.jackson3.api.score.buildin.hardsoft;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class HardSoftScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<HardSoftScore> {

    @Override
    public HardSoftScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return HardSoftScore.parseScore(parser.getValueAsString());
    }

}
