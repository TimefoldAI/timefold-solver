package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.Score;

import tools.jackson.databind.ValueDeserializer;

/**
 * Jackson binding support for a {@link Score} type.
 * <p>
 * For example: use
 * {@code @JsonSerialize(using = HardSoftScoreJacksonSerializer.class) @JsonDeserialize(using = HardSoftScoreJacksonDeserializer.class)}
 * on a {@code HardSoftScore score} field and it will marshalled to JSON as {@code "score":"-999hard/-999soft"}.
 *
 * @see Score
 * @param <Score_> the actual score type
 */
public abstract class AbstractScoreJacksonDeserializer<Score_ extends Score<Score_>>
        extends ValueDeserializer<Score_> {

}
