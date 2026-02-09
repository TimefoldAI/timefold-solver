package ai.timefold.solver.jackson.api.score.buildin.bendablelong;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class BendableLongScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<BendableLongScore> {

    @Override
    public BendableLongScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return BendableLongScore.parseScore(parser.getValueAsString());
    }

}
