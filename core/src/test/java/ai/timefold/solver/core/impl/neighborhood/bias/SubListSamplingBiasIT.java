package ai.timefold.solver.core.impl.neighborhood.bias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;

/**
 * {@code SubListSampler} composes {@code TriangleElementFactory}'s copied triangle bias with the pinned-prefix offset
 * and the value-seeded entity choice. The triangle math itself is copied verbatim from the legacy selector
 * and is not retested here; what is new is the composition, and only a direct statistical test proves the offset
 * and the seed-to-entity mapping did not introduce a bias of their own. Drives {@link SubListSampler} directly,
 * against a {@link MoveDirector} built from a real score director, so no move provider or
 * {@code NeighborhoodTester} is involved.
 */
class SubListSamplingBiasIT extends AbstractBiasIT {

    private static final int TRIAL_COUNT = 200_000;

    /**
     * One entity, list size 7, minimum 2, maximum 5: the worked example in {@code TriangleElementFactoryTest}
     * (nthTriangle(6) - nthTriangle(2) = 21 - 3 = 18 admissible (fromIndex, length) pairs,
     * each expected to be drawn with equal probability).
     */
    @Test
    void drawnSpanIsUniformOverEveryAdmissiblePair() {
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var values = new TestdataAllowsUnassignedValuesListValue[7];
        for (var i = 0; i < 7; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", values);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(solutionDescriptor,
                new TestdataAllowsUnassignedValuesListEasyScoreCalculator(), EnvironmentMode.PHASE_ASSERT);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var solutionView = new MoveDirector<>(scoreDirector);

        var sampler = Samplers.subList(variableMetaModel, 2, 5, new Random(0));
        var seedValue = values[0];

        var expectedPairSet = new HashSet<String>();
        for (var length = 2; length <= 5; length++) {
            for (var fromIndex = 0; fromIndex <= 7 - length; fromIndex++) {
                expectedPairSet.add(fromIndex + ".." + (fromIndex + length));
            }
        }
        assertThat(expectedPairSet).hasSize(18);

        BiasReport.tally("SubListSampler, uniform over (fromIndex, length)", TRIAL_COUNT, trial -> {
            var range = Objects.requireNonNull(sampler.byValue(solutionView, seedValue));
            return range.fromIndex() + ".." + range.toIndex();
        }).expectUniform(expectedPairSet).assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * Three entities with unpinned windows of 2, 3 and 5 values (10 total), minimum sub-list size 1
     * so every draw succeeds and cannot skew the tally: proves the value-seeded entity choice is weighted by unpinned size,
     * exactly as {@code RandomSubListSelector}'s own javadoc documents for the legacy selector
     * ("entities with bigger list variables will be selected more often").
     * The seed value is drawn uniformly over every assigned value, mirroring how
     * {@code MoveStreamFactory#forEachAssignedValue} feeds a real move provider.
     */
    @Test
    void drawnEntityIsWeightedByUnpinnedSize() {
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var smallValues = newValues("s", 2);
        var smallEntity = new TestdataAllowsUnassignedValuesListEntity("small", smallValues);
        var mediumValues = newValues("m", 3);
        var mediumEntity = new TestdataAllowsUnassignedValuesListEntity("medium", mediumValues);
        var largeValues = newValues("l", 5);
        var largeEntity = new TestdataAllowsUnassignedValuesListEntity("large", largeValues);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(smallEntity, mediumEntity, largeEntity));
        var allValues = new ArrayList<TestdataAllowsUnassignedValuesListValue>();
        allValues.addAll(List.of(smallValues));
        allValues.addAll(List.of(mediumValues));
        allValues.addAll(List.of(largeValues));
        solution.setValueList(allValues);

        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(solutionDescriptor,
                new TestdataAllowsUnassignedValuesListEasyScoreCalculator(), EnvironmentMode.PHASE_ASSERT);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var solutionView = new MoveDirector<>(scoreDirector);

        var sampler = Samplers.subList(variableMetaModel, 1, Integer.MAX_VALUE, new Random(0));
        var seedPicker = new Random(1);

        var weightByEntity = new HashMap<TestdataAllowsUnassignedValuesListEntity, Double>();
        weightByEntity.put(smallEntity, 0.2);
        weightByEntity.put(mediumEntity, 0.3);
        weightByEntity.put(largeEntity, 0.5);

        BiasReport.tally("SubListSampler, entity weighted by unpinned size", TRIAL_COUNT, trial -> {
            var seedValue = allValues.get(seedPicker.nextInt(allValues.size()));
            var range = Objects.requireNonNull(sampler.byValue(solutionView, seedValue));
            return range.<TestdataAllowsUnassignedValuesListEntity> entity();
        }).expectWeights(weightByEntity).assertWithinSigma(SIGMA_LIMIT);
    }

    private static TestdataAllowsUnassignedValuesListValue[] newValues(String prefix, int count) {
        var values = new TestdataAllowsUnassignedValuesListValue[count];
        for (var i = 0; i < count; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue(prefix + i);
        }
        return values;
    }

}
