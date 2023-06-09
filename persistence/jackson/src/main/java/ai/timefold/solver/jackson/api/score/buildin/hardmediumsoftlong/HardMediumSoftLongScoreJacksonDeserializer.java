package ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftlong;

import java.io.IOException;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

public class HardMediumSoftLongScoreJacksonDeserializer
        extends AbstractScoreJacksonDeserializer<HardMediumSoftLongScore> {

    @Override
    public HardMediumSoftLongScore deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return HardMediumSoftLongScore.parseScore(parser.getValueAsString());
    }

}
