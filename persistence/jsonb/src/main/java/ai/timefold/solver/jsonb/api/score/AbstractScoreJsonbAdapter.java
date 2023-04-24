package ai.timefold.solver.jsonb.api.score;

import jakarta.json.bind.adapter.JsonbAdapter;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.jsonb.api.TimefoldJsonbConfig;

/**
 * JSON-B binding support for a {@link Score} type.
 * <p>
 * For example: use {@code @JsonbTypeAdapter(HardSoftScoreJsonbAdapter.class)}
 * on a {@code HardSoftScore score} field and it will be serialized to JSON as {@code "score":"-999hard/-999soft"}.
 * Or better yet, use {@link TimefoldJsonbConfig} instead.
 *
 * @see Score
 * @param <Score_> the actual score type
 */
public abstract class AbstractScoreJsonbAdapter<Score_ extends Score<Score_>> implements JsonbAdapter<Score_, String> {

    @Override
    public String adaptToJson(Score_ score) {
        return score.toString();
    }
}
