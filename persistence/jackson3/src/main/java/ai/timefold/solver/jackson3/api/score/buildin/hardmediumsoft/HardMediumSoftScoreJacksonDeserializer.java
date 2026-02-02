package ai.timefold.solver.jackson3.api.score.buildin.hardmediumsoft;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class HardMediumSoftScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<HardMediumSoftScore> {

    @Override
    public HardMediumSoftScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return HardMediumSoftScore.parseScore(parser.getValueAsString());
    }

}
