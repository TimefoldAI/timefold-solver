package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftScoreJacksonSerializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Jackson binding support for a {@link Score} type (but not a subtype).
 * For a {@link Score} subtype field, use {@link HardSoftScoreJacksonSerializer} or similar instead.
 * <p>
 * For example: use
 * {@code @JsonSerialize(using = PolymorphicScoreJacksonSerializer.class) @JsonDeserialize(using = PolymorphicScoreJacksonDeserializer.class)}
 * on a {@code Score score} field which contains a {@link HardSoftScore} instance
 * and it will marshalled to JSON as {@code "score":{"type":"HARD_SOFT",score:"-999hard/-999soft"}}.
 *
 * @see Score
 * @see PolymorphicScoreJacksonDeserializer
 */
public class PolymorphicScoreJacksonSerializer extends ValueSerializer<Score> {

    @Override
    public void serialize(Score score, JsonGenerator generator, SerializationContext serializers) throws JacksonException {
        generator.writeStartObject();
        generator.writeStringProperty(score.getClass().getSimpleName(), score.toString());
        generator.writeEndObject();
    }

}
