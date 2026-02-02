package ai.timefold.solver.jackson3.api.score.buildin.hardmediumsoftlong;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class HardMediumSoftLongScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardMediumSoftLongScore> {

    @Override
    public HardMediumSoftLongScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return HardMediumSoftLongScore.parseScore(parser.getValueAsString());
    }

}
