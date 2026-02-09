package ai.timefold.solver.jackson.api.score.buildin.hardsoftlong;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class HardSoftLongScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<HardSoftLongScore> {

    @Override
    public HardSoftLongScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return HardSoftLongScore.parseScore(parser.getValueAsString());
    }

}
