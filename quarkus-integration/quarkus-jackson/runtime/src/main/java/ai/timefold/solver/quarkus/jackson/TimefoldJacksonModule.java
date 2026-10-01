package ai.timefold.solver.quarkus.jackson;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.domain.solution.DefaultConstraintWeightOverrides;
import ai.timefold.solver.quarkus.jackson.domain.solution.ConstraintWeightOverridesSerializer;
import ai.timefold.solver.quarkus.jackson.score.BendableBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.BendableBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.BendableScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.BendableScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.HardMediumSoftBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.HardMediumSoftBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.HardMediumSoftScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.HardMediumSoftScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.HardSoftBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.HardSoftBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.HardSoftScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.HardSoftScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.PolymorphicScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.PolymorphicScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.SimpleBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.SimpleBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.SimpleScoreJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.SimpleScoreJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.constraint.ConstraintRefJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.constraint.ConstraintRefJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.BreakJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.BreakJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.LoadBalanceJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.LoadBalanceJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.SequenceChainJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.SequenceChainJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.SequenceJacksonDeserializer;
import ai.timefold.solver.quarkus.jackson.score.stream.common.SequenceJacksonSerializer;
import ai.timefold.solver.quarkus.jackson.solution.JacksonSolutionFileIO;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class adds all Jackson serializers and deserializers.
 */
public class TimefoldJacksonModule extends SimpleModule {

    /**
     * Jackson modules can be loaded automatically via {@link java.util.ServiceLoader}.
     * This will happen if you use {@link JacksonSolutionFileIO}.
     * Otherwise, register the module with {@link ObjectMapper#registerModule(Module)}.
     *
     * @return never null
     */
    public static Module createModule() {
        return new TimefoldJacksonModule();

    }

    public TimefoldJacksonModule() {
        this("Timefold");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TimefoldJacksonModule(String name) {
        super(name);
        // For non-subtype Score fields/properties, we also need to record the score type
        addSerializer(Score.class, new PolymorphicScoreJacksonSerializer());
        addDeserializer(Score.class, new PolymorphicScoreJacksonDeserializer());

        addSerializer(SimpleScore.class, new SimpleScoreJacksonSerializer());
        addDeserializer(SimpleScore.class, new SimpleScoreJacksonDeserializer());
        addSerializer(SimpleBigDecimalScore.class, new SimpleBigDecimalScoreJacksonSerializer());
        addDeserializer(SimpleBigDecimalScore.class, new SimpleBigDecimalScoreJacksonDeserializer());
        addSerializer(HardSoftScore.class, new HardSoftScoreJacksonSerializer());
        addDeserializer(HardSoftScore.class, new HardSoftScoreJacksonDeserializer());
        addSerializer(HardSoftBigDecimalScore.class, new HardSoftBigDecimalScoreJacksonSerializer());
        addDeserializer(HardSoftBigDecimalScore.class, new HardSoftBigDecimalScoreJacksonDeserializer());
        addSerializer(HardMediumSoftScore.class, new HardMediumSoftScoreJacksonSerializer());
        addDeserializer(HardMediumSoftScore.class, new HardMediumSoftScoreJacksonDeserializer());
        addSerializer(HardMediumSoftBigDecimalScore.class, new HardMediumSoftBigDecimalScoreJacksonSerializer());
        addDeserializer(HardMediumSoftBigDecimalScore.class, new HardMediumSoftBigDecimalScoreJacksonDeserializer());
        addSerializer(BendableScore.class, new BendableScoreJacksonSerializer());
        addDeserializer(BendableScore.class, new BendableScoreJacksonDeserializer());
        addSerializer(BendableBigDecimalScore.class, new BendableBigDecimalScoreJacksonSerializer());
        addDeserializer(BendableBigDecimalScore.class, new BendableBigDecimalScoreJacksonDeserializer());

        // Constraint weights
        addSerializer(ConstraintRef.class, new ConstraintRefJacksonSerializer());
        addDeserializer(ConstraintRef.class, new ConstraintRefJacksonDeserializer());
        addSerializer(ConstraintWeightOverrides.class, new ConstraintWeightOverridesSerializer());
        addSerializer(DefaultConstraintWeightOverrides.class, new ConstraintWeightOverridesSerializer());

        // Constraint collectors
        addSerializer(Break.class, new BreakJacksonSerializer());
        addDeserializer(Break.class, new BreakJacksonDeserializer<>());
        addSerializer(Sequence.class, new SequenceJacksonSerializer());
        addDeserializer(Sequence.class, new SequenceJacksonDeserializer<>());
        addSerializer(SequenceChain.class, new SequenceChainJacksonSerializer());
        addDeserializer(SequenceChain.class, new SequenceChainJacksonDeserializer<>());
        addSerializer(LoadBalance.class, new LoadBalanceJacksonSerializer());
        addDeserializer(LoadBalance.class, new LoadBalanceJacksonDeserializer<>());
    }

}
