package ai.timefold.solver.jsonb.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;

import ai.timefold.solver.jsonb.api.score.buildin.bendable.BendableScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.bendablelong.BendableLongScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.hardmediumsoft.HardMediumSoftScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.hardsoft.HardSoftScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.hardsoftlong.HardSoftLongScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.simple.SimpleScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreJsonbAdapter;
import ai.timefold.solver.jsonb.api.score.buildin.simplelong.SimpleLongScoreJsonbAdapter;

/**
 * This class adds all JSON-B adapters.
 */
public class TimefoldJsonbConfig {

    /**
     * @return never null, use it to create a {@link Jsonb} instance with {@link JsonbBuilder#create(JsonbConfig)}.
     */
    public static JsonbConfig createConfig() {
        JsonbConfig config = new JsonbConfig()
                .withAdapters(new BendableScoreJsonbAdapter(),
                        new BendableBigDecimalScoreJsonbAdapter(),
                        new BendableLongScoreJsonbAdapter(),
                        new HardMediumSoftScoreJsonbAdapter(),
                        new HardMediumSoftBigDecimalScoreJsonbAdapter(),
                        new HardMediumSoftLongScoreJsonbAdapter(),
                        new HardSoftScoreJsonbAdapter(),
                        new HardSoftBigDecimalScoreJsonbAdapter(),
                        new HardSoftLongScoreJsonbAdapter(),
                        new SimpleScoreJsonbAdapter(),
                        new SimpleBigDecimalScoreJsonbAdapter(),
                        new SimpleLongScoreJsonbAdapter());

        return config;
    }

    /**
     * @return never null, use it to customize a {@link JsonbConfig} instance with
     *         {@link JsonbConfig#withAdapters(JsonbAdapter[])}.
     */
    public static JsonbAdapter[] getScoreJsonbAdapters() {
        return new JsonbAdapter[] {
                new BendableScoreJsonbAdapter(),
                new BendableBigDecimalScoreJsonbAdapter(),
                new BendableLongScoreJsonbAdapter(),
                new HardMediumSoftScoreJsonbAdapter(),
                new HardMediumSoftBigDecimalScoreJsonbAdapter(),
                new HardMediumSoftLongScoreJsonbAdapter(),
                new HardSoftScoreJsonbAdapter(),
                new HardSoftBigDecimalScoreJsonbAdapter(),
                new HardSoftLongScoreJsonbAdapter(),
                new SimpleScoreJsonbAdapter(),
                new SimpleBigDecimalScoreJsonbAdapter(),
                new SimpleLongScoreJsonbAdapter() };
    }
}
