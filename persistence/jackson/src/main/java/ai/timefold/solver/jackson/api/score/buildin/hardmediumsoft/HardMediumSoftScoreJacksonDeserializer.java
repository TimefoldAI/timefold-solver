package ai.timefold.solver.jackson.api.score.buildin.hardmediumsoft;

import java.io.IOException;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class HardMediumSoftScoreJacksonDeserializer extends AbstractScoreJacksonDeserializer<HardMediumSoftScore> {

    @Override
    public HardMediumSoftScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return HardMediumSoftScore.parseScore(parser.getValueAsString());
    }

}
