package ai.timefold.solver.jackson.api;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.jackson.api.score.PolymorphicScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.PolymorphicScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.bendable.BendableScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.bendable.BendableScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.bendablelong.BendableLongScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.bendablelong.BendableLongScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoft.HardMediumSoftScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoft.HardMediumSoftScoreJsonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.hardsoft.HardSoftScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.hardsoft.HardSoftScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.hardsoftlong.HardSoftLongScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.hardsoftlong.HardSoftLongScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.simple.SimpleScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.simple.SimpleScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.simplelong.SimpleLongScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.simplelong.SimpleLongScoreJacksonSerializer;
import ai.timefold.solver.jackson.impl.domain.solution.JacksonSolutionFileIO;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class adds all Jackson serializers and deserializers.
 */
public class OptaPlannerJacksonModule extends SimpleModule {

    /**
     * Jackson modules can be loaded automatically via {@link java.util.ServiceLoader}.
     * This will happen if you use {@link JacksonSolutionFileIO}.
     * Otherwise, register the module with {@link ObjectMapper#registerModule(Module)}.
     *
     * @return never null
     */
    public static Module createModule() {
        return new OptaPlannerJacksonModule();

    }

    /**
     * @deprecated Have the module loaded automatically via {@link JacksonSolutionFileIO} or use {@link #createModule()}.
     *             This constructor will be hidden in a future major version of OptaPlanner.
     */
    @Deprecated(forRemoval = true)
    public OptaPlannerJacksonModule() {
        super("OptaPlanner");
        // For non-subtype Score fields/properties, we also need to record the score type
        addSerializer(Score.class, new PolymorphicScoreJacksonSerializer());
        addDeserializer(Score.class, new PolymorphicScoreJacksonDeserializer());

        addSerializer(SimpleScore.class, new SimpleScoreJacksonSerializer());
        addDeserializer(SimpleScore.class, new SimpleScoreJacksonDeserializer());
        addSerializer(SimpleLongScore.class, new SimpleLongScoreJacksonSerializer());
        addDeserializer(SimpleLongScore.class, new SimpleLongScoreJacksonDeserializer());
        addSerializer(SimpleBigDecimalScore.class, new SimpleBigDecimalScoreJacksonSerializer());
        addDeserializer(SimpleBigDecimalScore.class, new SimpleBigDecimalScoreJacksonDeserializer());
        addSerializer(HardSoftScore.class, new HardSoftScoreJacksonSerializer());
        addDeserializer(HardSoftScore.class, new HardSoftScoreJacksonDeserializer());
        addSerializer(HardSoftLongScore.class, new HardSoftLongScoreJacksonSerializer());
        addDeserializer(HardSoftLongScore.class, new HardSoftLongScoreJacksonDeserializer());
        addSerializer(HardSoftBigDecimalScore.class, new HardSoftBigDecimalScoreJacksonSerializer());
        addDeserializer(HardSoftBigDecimalScore.class, new HardSoftBigDecimalScoreJacksonDeserializer());
        addSerializer(HardMediumSoftScore.class, new HardMediumSoftScoreJsonSerializer());
        addDeserializer(HardMediumSoftScore.class, new HardMediumSoftScoreJacksonDeserializer());
        addSerializer(HardMediumSoftLongScore.class, new HardMediumSoftLongScoreJacksonSerializer());
        addDeserializer(HardMediumSoftLongScore.class, new HardMediumSoftLongScoreJacksonDeserializer());
        addSerializer(HardMediumSoftBigDecimalScore.class, new HardMediumSoftBigDecimalScoreJacksonSerializer());
        addDeserializer(HardMediumSoftBigDecimalScore.class, new HardMediumSoftBigDecimalScoreJacksonDeserializer());
        addSerializer(BendableScore.class, new BendableScoreJacksonSerializer());
        addDeserializer(BendableScore.class, new BendableScoreJacksonDeserializer());
        addSerializer(BendableLongScore.class, new BendableLongScoreJacksonSerializer());
        addDeserializer(BendableLongScore.class, new BendableLongScoreJacksonDeserializer());
        addSerializer(BendableBigDecimalScore.class, new BendableBigDecimalScoreJacksonSerializer());
        addDeserializer(BendableBigDecimalScore.class, new BendableBigDecimalScoreJacksonDeserializer());
    }

}
