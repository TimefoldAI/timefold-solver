package ai.timefold.solver.jackson.api;

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
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.solver.RecommendedAssignment;
import ai.timefold.solver.core.impl.domain.solution.DefaultConstraintWeightOverrides;
import ai.timefold.solver.core.impl.solver.DefaultRecommendedAssignment;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningVariableDiff;
import ai.timefold.solver.jackson.api.domain.solution.ConstraintWeightOverridesSerializer;
import ai.timefold.solver.jackson.api.score.PolymorphicScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.PolymorphicScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.analysis.ScoreAnalysisJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.BendableBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.BendableBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.BendableScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.BendableScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.SimpleBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.SimpleBigDecimalScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.buildin.SimpleScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.SimpleScoreJacksonSerializer;
import ai.timefold.solver.jackson.api.score.constraint.ConstraintRefJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.constraint.ConstraintRefJacksonSerializer;
import ai.timefold.solver.jackson.api.score.stream.common.BreakJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.stream.common.BreakJacksonSerializer;
import ai.timefold.solver.jackson.api.score.stream.common.LoadBalanceJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.stream.common.LoadBalanceJacksonSerializer;
import ai.timefold.solver.jackson.api.score.stream.common.SequenceChainJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.stream.common.SequenceChainJacksonSerializer;
import ai.timefold.solver.jackson.api.score.stream.common.SequenceJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.stream.common.SequenceJacksonSerializer;
import ai.timefold.solver.jackson.api.solver.RecommendedAssignmentJacksonSerializer;
import ai.timefold.solver.jackson.impl.domain.solution.JacksonSolutionFileIO;
import ai.timefold.solver.jackson.preview.api.domain.solution.diff.PlanningEntityDiffJacksonSerializer;
import ai.timefold.solver.jackson.preview.api.domain.solution.diff.PlanningSolutionDiffJacksonSerializer;
import ai.timefold.solver.jackson.preview.api.domain.solution.diff.PlanningVariableDiffJacksonSerializer;

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * This class adds all Jackson serializers and deserializers.
 */
public class TimefoldJacksonModule extends SimpleModule {

    /**
     * Jackson modules can be loaded automatically via {@link java.util.ServiceLoader}.
     * This will happen if you use {@link JacksonSolutionFileIO}.
     * Otherwise, register the module with {@link JsonMapper.Builder#addModule(JacksonModule)}.
     *
     * @return never null
     */
    public static JacksonModule createModule() {
        return new TimefoldJacksonModule();

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TimefoldJacksonModule() {
        super("Timefold");
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

        // Score analysis
        addSerializer(ConstraintRef.class, new ConstraintRefJacksonSerializer());
        addDeserializer(ConstraintRef.class, new ConstraintRefJacksonDeserializer());
        addSerializer(ScoreAnalysis.class, new ScoreAnalysisJacksonSerializer());
        var serializer = (ValueSerializer) new RecommendedAssignmentJacksonSerializer<>();
        addSerializer(RecommendedAssignment.class, serializer);
        addSerializer(DefaultRecommendedAssignment.class, serializer);

        // Constraint weights
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

        // Solution diff
        addSerializer(PlanningSolutionDiff.class, new PlanningSolutionDiffJacksonSerializer());
        addSerializer(PlanningEntityDiff.class, new PlanningEntityDiffJacksonSerializer());
        addSerializer(PlanningVariableDiff.class, new PlanningVariableDiffJacksonSerializer());
    }

}
