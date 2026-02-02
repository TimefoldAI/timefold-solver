package ai.timefold.solver.jackson3.api.score.buildin.simplelong;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

public class SimpleLongScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<SimpleLongScore> {

    @Override
    public SimpleLongScore deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        return SimpleLongScore.parseScore(parser.getValueAsString());
    }

}
