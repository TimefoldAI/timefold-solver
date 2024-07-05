package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedObject;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListCMAIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListEntityWithShadowHistory;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListSolutionWithShadowHistory;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListValueWithShadowHistory;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListWithShadowHistoryIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultivarIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataOtherValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedEntity;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedSolution;
import ai.timefold.solver.core.impl.util.Pair;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class SolutionManagerTest {

    public static final SolverFactory<TestdataShadowedSolution> SOLVER_FACTORY =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataShadowedSolverConfig.xml");
    public static final SolverFactory<TestdataAllowsUnassignedSolution> SOLVER_FACTORY_OVERCONSTRAINED =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataOverconstrainedSolverConfig.xml");
    public static final SolverFactory<TestdataShadowedSolution> SOLVER_FACTORY_SHADOWED = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataShadowedSolution.class)
                    .withEntityClasses(TestdataShadowedEntity.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig().withIncrementalScoreCalculatorClass(
                                    TestdataShadowedIncrementalScoreCalculator.class)));
    public static final SolverFactory<TestdataAllowsUnassignedSolution> SOLVER_FACTORY_UNASSIGNED = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataAllowsUnassignedSolution.class)
                    .withEntityClasses(TestdataAllowsUnassignedEntity.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig().withIncrementalScoreCalculatorClass(
                                    TestdataAllowsUnassignedIncrementalScoreCalculator.class)));
    public static final SolverFactory<TestdataMultiVarSolution> SOLVER_FACTORY_MULTIVAR = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataMultiVarSolution.class)
                    .withEntityClasses(TestdataMultiVarEntity.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig().withIncrementalScoreCalculatorClass(
                                    TestdataMultivarIncrementalScoreCalculator.class)));
    public static final SolverFactory<TestdataShadowingChainedSolution> SOLVER_FACTORY_CHAINED = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataShadowingChainedSolution.class)
                    .withEntityClasses(TestdataShadowingChainedEntity.class, TestdataShadowingChainedObject.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig().withIncrementalScoreCalculatorClass(
                                    TestdataShadowingChainedIncrementalScoreCalculator.class)));
    public static final SolverFactory<TestdataListSolutionWithShadowHistory> SOLVER_FACTORY_LIST = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataListSolutionWithShadowHistory.class)
                    .withEntityClasses(TestdataListEntityWithShadowHistory.class, TestdataListValueWithShadowHistory.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig().withIncrementalScoreCalculatorClass(
                                    TestdataListWithShadowHistoryIncrementalScoreCalculator.class)));
    public static final SolverFactory<TestdataPinnedWithIndexListSolution> SOLVER_FACTORY_LIST_PINNED = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataPinnedWithIndexListSolution.class)
                    .withEntityClasses(TestdataPinnedWithIndexListEntity.class, TestdataPinnedWithIndexListValue.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig().withIncrementalScoreCalculatorClass(
                                    TestdataPinnedWithIndexListCMAIncrementalScoreCalculator.class)));

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverything(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNotNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverythingChained(SolutionManagerSource SolutionManagerSource) {
        var a0 = new TestdataShadowingChainedAnchor("a0");
        var a1 = new TestdataShadowingChainedEntity("a1", a0);
        var b0 = new TestdataShadowingChainedAnchor("b0");
        var b1 = new TestdataShadowingChainedEntity("b1", b0);
        var b2 = new TestdataShadowingChainedEntity("b2", b1);
        var c0 = new TestdataShadowingChainedAnchor("c0");
        var solution = new TestdataShadowingChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0, c0));
        solution.setChainedEntityList(Arrays.asList(a1, b1, b2));

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(a0.getNextEntity()).isNull();
            softly.assertThat(a1.getAnchor()).isNull();
            softly.assertThat(a1.getNextEntity()).isNull();
            softly.assertThat(b0.getNextEntity()).isNull();
            softly.assertThat(b1.getAnchor()).isNull();
            softly.assertThat(b1.getNextEntity()).isNull();
            softly.assertThat(b2.getAnchor()).isNull();
            softly.assertThat(b2.getNextEntity()).isNull();
            softly.assertThat(c0.getNextEntity()).isNull();
        });

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_CHAINED);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            softly.assertThat(a0.getNextEntity()).isEqualTo(a1);
            softly.assertThat(a1.getAnchor()).isEqualTo(a0);
            softly.assertThat(a1.getNextEntity()).isNull();
            softly.assertThat(b0.getNextEntity()).isEqualTo(b1);
            softly.assertThat(b1.getAnchor()).isEqualTo(b0);
            softly.assertThat(b1.getNextEntity()).isEqualTo(b2);
            softly.assertThat(b2.getAnchor()).isEqualTo(b0);
            softly.assertThat(b2.getNextEntity()).isNull();
            softly.assertThat(c0.getNextEntity()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverythingList(SolutionManagerSource SolutionManagerSource) {
        var a0 = new TestdataListValueWithShadowHistory("a0");
        var a1 = new TestdataListValueWithShadowHistory("a1");
        var a = new TestdataListEntityWithShadowHistory("a", a0, a1);
        var b0 = new TestdataListValueWithShadowHistory("b0");
        var b1 = new TestdataListValueWithShadowHistory("b1");
        var b2 = new TestdataListValueWithShadowHistory("b2");
        var b = new TestdataListEntityWithShadowHistory("b", b0, b1, b2);
        var c0 = new TestdataListValueWithShadowHistory("c0");
        var c = new TestdataListEntityWithShadowHistory("c", c0);
        var d = new TestdataListEntityWithShadowHistory("d");
        var solution = new TestdataListSolutionWithShadowHistory();
        solution.setEntityList(Arrays.asList(a, b, c, d));
        solution.setValueList(Arrays.asList(a0, a1, b0, b1, b2, c0));

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            assertShadowedListValueAllNull(softly, a0);
            assertShadowedListValueAllNull(softly, a1);
            assertShadowedListValueAllNull(softly, b0);
            assertShadowedListValueAllNull(softly, b1);
            assertShadowedListValueAllNull(softly, b2);
            assertShadowedListValueAllNull(softly, c0);
        });

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_LIST);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            assertShadowedListValue(softly, a0, a, 0, null, a1);
            assertShadowedListValue(softly, a1, a, 1, a0, null);
            assertShadowedListValue(softly, b0, b, 0, null, b1);
            assertShadowedListValue(softly, b1, b, 1, b0, b2);
            assertShadowedListValue(softly, b2, b, 2, b1, null);
            assertShadowedListValue(softly, c0, c, 0, null, null);
        });
    }

    private void assertShadowedListValueAllNull(SoftAssertions softly, TestdataListValueWithShadowHistory current) {
        softly.assertThat(current.getIndex()).isNull();
        softly.assertThat(current.getEntity()).isNull();
        softly.assertThat(current.getPrevious()).isNull();
        softly.assertThat(current.getNext()).isNull();
    }

    private void assertShadowedListValue(SoftAssertions softly, TestdataListValueWithShadowHistory current,
            TestdataListEntityWithShadowHistory entity, int index, TestdataListValueWithShadowHistory previous,
            TestdataListValueWithShadowHistory next) {
        softly.assertThat(current.getIndex()).isEqualTo(index);
        softly.assertThat(current.getEntity()).isEqualTo(entity);
        softly.assertThat(current.getPrevious()).isEqualTo(previous);
        softly.assertThat(current.getNext()).isEqualTo(next);
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateOnlyShadowVariables(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SHADOW_VARIABLES_ONLY);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNotNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateOnlyScore(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void explain(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(solutionManager).isNotNull();

        var scoreExplanation = solutionManager.explain(solution);
        assertThat(scoreExplanation).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(scoreExplanation.getScore()).isNotNull();
            softly.assertThat(scoreExplanation.getSummary()).isNotBlank();
            softly.assertThat(scoreExplanation.getConstraintMatchTotalMap())
                    .containsOnlyKeys("ai.timefold.solver.core.impl.testdata.domain.shadow/testConstraint");
            softly.assertThat(scoreExplanation.getIndictmentMap())
                    .containsOnlyKeys(solution.getEntityList().toArray());

        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void analyze(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(solutionManager).isNotNull();

        var scoreAnalysis = solutionManager.analyze(solution);
        assertThat(scoreAnalysis).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(scoreAnalysis.score()).isNotNull();
            softly.assertThat(scoreAnalysis.constraintMap()).isNotEmpty();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void analyzeNonNullableWithNullValue(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();
        solution.getEntityList().get(0).setValue(null);

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(solutionManager).isNotNull();

        assertThat(solutionManager.analyze(solution)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void analyzeNullableWithNullValue(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataAllowsUnassignedSolution.generateSolution();
        solution.getEntityList().get(0).setValue(null);

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_OVERCONSTRAINED);
        assertThat(solutionManager).isNotNull();

        var scoreAnalysis = solutionManager.analyze(solution);
        assertThat(scoreAnalysis).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(scoreAnalysis.score()).isNotNull();
            softly.assertThat(scoreAnalysis.constraintMap()).isNotEmpty();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void analyzeWithUninitializedSolution(SolutionManagerSource SolutionManagerSource) {
        var uninitializedSolution = TestdataShadowedSolution.generateSolution(3, 3);
        uninitializedSolution.getEntityList().forEach(e -> e.setValue(null));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(solutionManager).isNotNull();

        var scoreAnalysis = solutionManager.analyze(uninitializedSolution);
        assertThat(scoreAnalysis).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(scoreAnalysis.score()).isNotNull();
            softly.assertThat(scoreAnalysis.constraintMap()).isNotEmpty();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFit(SolutionManagerSource SolutionManagerSource) {
        int valueSize = 3;
        var solution = TestdataShadowedSolution.generateSolution(valueSize, 3);
        var uninitializedEntity = solution.getEntityList().get(2);
        var unassignedValue = uninitializedEntity.getValue();
        uninitializedEntity.setValue(null);

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        var recommendationList = solutionManager.recommendFit(solution, uninitializedEntity, TestdataShadowedEntity::getValue);

        // Three values means there need to be three recommendations.
        assertThat(recommendationList).hasSize(valueSize);
        /*
         * The calculator counts how many entities have the same value as another entity.
         * Therefore the recommendation to assign value #2 needs to come first,
         * as it means each entity only has each value once.
         */
        var firstRecommendation = recommendationList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstRecommendation.proposition()).isEqualTo(unassignedValue);
            softly.assertThat(firstRecommendation.scoreAnalysisDiff()
                    .score()).isEqualTo(SimpleScore.of(-1));
        });
        // The other two recommendations need to come in order of the placer; so value #0, then value #1.
        var secondRecommendation = recommendationList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondRecommendation.proposition()).isEqualTo(solution.getValueList().get(0));
            softly.assertThat(secondRecommendation.scoreAnalysisDiff()
                    .score()).isEqualTo(SimpleScore.of(-3));
        });
        var thirdRecommendation = recommendationList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdRecommendation.proposition()).isEqualTo(solution.getValueList().get(1));
            softly.assertThat(thirdRecommendation.scoreAnalysisDiff()
                    .score()).isEqualTo(SimpleScore.of(-3));
        });
        // Ensure the original solution is in its original state.
        assertSoftly(softly -> {
            softly.assertThat(uninitializedEntity.getValue()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getValue()).isEqualTo(solution.getValueList().get(0));
            softly.assertThat(solution.getEntityList().get(1).getValue()).isEqualTo(solution.getValueList().get(1));
            softly.assertThat(solution.getScore()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitUninitializedSolution(SolutionManagerSource SolutionManagerSource) {
        int valueSize = 3;
        var uninitializedSolution = TestdataShadowedSolution.generateSolution(valueSize, 3);
        uninitializedSolution.getEntityList().forEach(e -> e.setValue(null));
        var uninitializedEntity = uninitializedSolution.getEntityList().get(2);

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        assertThatThrownBy(() -> solutionManager.recommendFit(uninitializedSolution, uninitializedEntity,
                TestdataShadowedEntity::getValue))
                .hasMessageContaining("Solution (Generated Solution 0) has (3) uninitialized elements.");
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitWithUnassigned(SolutionManagerSource SolutionManagerSource) {
        int valueSize = 3;
        var solution = TestdataAllowsUnassignedSolution.generateSolution(valueSize, 3);
        var uninitializedEntity = solution.getEntityList().get(2);
        uninitializedEntity.setValue(null);

        // At this point, entity 0 and entity 2 are unassigned.
        // Entity 1 is assigned to value #1.
        // But only entity2 should be processed for recommendations.
        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_UNASSIGNED);
        assertThat(solutionManager).isNotNull();
        var recommendationList =
                solutionManager.recommendFit(solution, uninitializedEntity, TestdataAllowsUnassignedEntity::getValue);

        // Three values means there need to be four recommendations, one extra for unassigned.
        assertThat(recommendationList).hasSize(valueSize + 1);
        /*
         * The calculator penalizes how many entities have the same value as another entity.
         * Therefore the recommendation to assign value 0 and value 2 need to come first and in the order of the placer,
         * as it means two entities no longer share a value, improving the score.
         */
        var recommendation1 = recommendationList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(recommendation1.proposition()).isEqualTo(solution.getValueList().get(0));
            softly.assertThat(recommendation1.scoreAnalysisDiff()
                    .score()).isEqualTo(SimpleScore.of(2)); // Two entities no longer share null value.
        });
        var recommendation2 = recommendationList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(recommendation2.proposition()).isEqualTo(solution.getValueList().get(2));
            softly.assertThat(recommendation2.scoreAnalysisDiff()
                    .score()).isEqualTo(SimpleScore.of(2));
        });
        // The other two recommendations need to come in order of the placer; so null, then value #1.
        var recommendation3 = recommendationList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(recommendation3.proposition()).isEqualTo(null);
            softly.assertThat(recommendation3.scoreAnalysisDiff()
                    .score()).isEqualTo(SimpleScore.ZERO);
        });
        var recommendation4 = recommendationList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(recommendation4.proposition()).isEqualTo(solution.getValueList().get(1));
            softly.assertThat(recommendation4.scoreAnalysisDiff()
                    .score()).isEqualTo(SimpleScore.ZERO);
        });
        // Ensure the original solution is in its original state.
        assertSoftly(softly -> {
            softly.assertThat(uninitializedEntity.getValue()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getValue()).isNull();
            softly.assertThat(solution.getEntityList().get(1).getValue()).isEqualTo(solution.getValueList().get(1));
            softly.assertThat(solution.getEntityList().get(2).getValue()).isNull();
            softly.assertThat(solution.getScore()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitUninitializedSolutionWithUnassigned(SolutionManagerSource SolutionManagerSource) {
        int valueSize = 3;
        var uninitializedSolution = TestdataAllowsUnassignedSolution.generateSolution(valueSize, 3);
        uninitializedSolution.getEntityList().forEach(e -> e.setValue(null));
        var uninitializedEntity = uninitializedSolution.getEntityList().get(2);
        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_UNASSIGNED);
        assertThat(solutionManager).isNotNull();
        var recommendationList = solutionManager.recommendFit(uninitializedSolution, uninitializedEntity,
                TestdataAllowsUnassignedEntity::getValue);

        // Three values means there need to be four recommendations, one extra for unassigned.
        assertThat(recommendationList).hasSize(valueSize + 1);
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitMultivar(SolutionManagerSource SolutionManagerSource) {
        var solution = new TestdataMultiVarSolution("solution");
        var firstValue = new TestdataValue("firstValue");
        var secondValue = new TestdataValue("secondValue");
        solution.setValueList(List.of(firstValue, secondValue));
        var firstOtherValue = new TestdataOtherValue("firstOtherValue");
        solution.setOtherValueList(List.of(firstOtherValue));
        var uninitializedEntity = new TestdataMultiVarEntity("uninitialized");
        solution.setMultiVarEntityList(List.of(uninitializedEntity));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_MULTIVAR);
        var recommendationList = solutionManager.recommendFit(solution, uninitializedEntity,
                entity -> new Triple<>(entity.getPrimaryValue(), entity.getSecondaryValue(),
                        entity.getTertiaryValueAllowedUnassigned()));
        assertThat(recommendationList).hasSize(8);

        var firstRecommendation = recommendationList.get(0);
        assertSoftly(softly -> {
            var propositition = firstRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(firstValue);
            softly.assertThat(propositition.b()).isSameAs(firstValue);
            softly.assertThat(propositition.c()).isNull();
            softly.assertThat(firstRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(0));
        });

        var secondRecommendation = recommendationList.get(1);
        assertSoftly(softly -> {
            var propositition = secondRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(secondValue);
            softly.assertThat(propositition.b()).isSameAs(secondValue);
            softly.assertThat(propositition.c()).isNull();
            softly.assertThat(secondRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(0));
        });

        var thirdRecommendation = recommendationList.get(2);
        assertSoftly(softly -> {
            var propositition = thirdRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(firstValue);
            softly.assertThat(propositition.b()).isSameAs(firstValue);
            softly.assertThat(propositition.c()).isSameAs(firstOtherValue);
            softly.assertThat(thirdRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-1));
        });

        var fourthRecommendation = recommendationList.get(3);
        assertSoftly(softly -> {
            var propositition = fourthRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(firstValue);
            softly.assertThat(propositition.b()).isSameAs(secondValue);
            softly.assertThat(propositition.c()).isNull();
            softly.assertThat(fourthRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-1));
        });

        var fifthRecommendation = recommendationList.get(4);
        assertSoftly(softly -> {
            var propositition = fifthRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(secondValue);
            softly.assertThat(propositition.b()).isSameAs(firstValue);
            softly.assertThat(propositition.c()).isNull();
            softly.assertThat(fifthRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-1));
        });

        var sixthRecommendation = recommendationList.get(5);
        assertSoftly(softly -> {
            var propositition = sixthRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(secondValue);
            softly.assertThat(propositition.b()).isSameAs(secondValue);
            softly.assertThat(propositition.c()).isSameAs(firstOtherValue);
            softly.assertThat(sixthRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-1));
        });

        var seventhRecommendation = recommendationList.get(6);
        assertSoftly(softly -> {
            var propositition = seventhRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(firstValue);
            softly.assertThat(propositition.b()).isSameAs(secondValue);
            softly.assertThat(propositition.c()).isSameAs(firstOtherValue);
            softly.assertThat(seventhRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-2));
        });

        var eighthRecommendation = recommendationList.get(7);
        assertSoftly(softly -> {
            var propositition = eighthRecommendation.proposition();
            softly.assertThat(propositition.a()).isSameAs(secondValue);
            softly.assertThat(propositition.b()).isSameAs(firstValue);
            softly.assertThat(propositition.c()).isSameAs(firstOtherValue);
            softly.assertThat(eighthRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-2));
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitUninitializedSolutionWithMultivar(SolutionManagerSource SolutionManagerSource) {
        var solution = new TestdataMultiVarSolution("solution");
        var firstValue = new TestdataValue("firstValue");
        var secondValue = new TestdataValue("secondValue");
        solution.setValueList(List.of(firstValue, secondValue));
        var firstOtherValue = new TestdataOtherValue("firstOtherValue");
        solution.setOtherValueList(List.of(firstOtherValue));
        var uninitializedEntity = new TestdataMultiVarEntity("uninitialized");
        var secondUninitializedEntity = new TestdataMultiVarEntity("uninitialized2");
        solution.setMultiVarEntityList(List.of(uninitializedEntity, secondUninitializedEntity));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_MULTIVAR);
        assertThatThrownBy(() -> solutionManager.recommendFit(solution, uninitializedEntity,
                entity -> new Triple<>(entity.getPrimaryValue(), entity.getSecondaryValue(),
                        entity.getTertiaryValueAllowedUnassigned())))
                .hasMessageContaining("Solution (solution) has (2) uninitialized elements.");
    }

    record Triple<A, B, C>(A a, B b, C c) {

    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitChained(SolutionManagerSource SolutionManagerSource) {
        var a0 = new TestdataShadowingChainedAnchor("a0");
        var b0 = new TestdataShadowingChainedAnchor("b0");
        var b1 = new TestdataShadowingChainedEntity("b1", b0);
        var c0 = new TestdataShadowingChainedAnchor("c0");
        var c1 = new TestdataShadowingChainedEntity("c1", c0);
        var c2 = new TestdataShadowingChainedEntity("c2", c1);
        var uninitializedEntity = new TestdataShadowingChainedEntity("uninitialized");
        var solution = new TestdataShadowingChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0, c0));
        solution.setChainedEntityList(Arrays.asList(b1, c1, c2, uninitializedEntity));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_CHAINED);
        var recommendationList =
                solutionManager.recommendFit(solution, uninitializedEntity, TestdataShadowingChainedEntity::getChainedObject);
        assertThat(recommendationList).hasSize(6);

        // First recommendation is to be added to the "a" chain, as that results in the shortest chain.
        var firstRecommendation = recommendationList.get(0);
        assertSoftly(softly -> {
            var clonedAnchor = (TestdataShadowingChainedAnchor) firstRecommendation.proposition();
            // The anchor is cloned...
            softly.assertThat(clonedAnchor).isNotEqualTo(a0);
            softly.assertThat(clonedAnchor.getCode()).isEqualTo(a0.getCode());
            // ... but it is in a state as it would've been in the original solution.
            softly.assertThat(clonedAnchor.getNextEntity()).isNull();
            softly.assertThat(firstRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-3));
        });

        // Second recommendation is to be added to the start of the "b" chain.
        var secondRecommendation = recommendationList.get(1);
        assertSoftly(softly -> {
            var clonedAnchor = (TestdataShadowingChainedAnchor) secondRecommendation.proposition();
            softly.assertThat(clonedAnchor).isNotEqualTo(b0);
            softly.assertThat(clonedAnchor.getCode()).isEqualTo(b0.getCode());
            softly.assertThat(clonedAnchor.getNextEntity().getCode()).isEqualTo(b1.getCode());
            softly.assertThat(secondRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-21));
        });

        // Third recommendation is to be added to the end of the "b" chain.
        var thirdRecommendation = recommendationList.get(2);
        assertSoftly(softly -> {
            var clonedEntity = (TestdataShadowingChainedEntity) thirdRecommendation.proposition();
            softly.assertThat(clonedEntity).isNotEqualTo(b1);
            softly.assertThat(clonedEntity.getCode()).isEqualTo(b1.getCode());
            softly.assertThat(clonedEntity.getNextEntity()).isNull();
            softly.assertThat(thirdRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-21));
        });

        // Fourth recommendation is to be added to the start of the "c" chain and so on...
        var fourthRecommendation = recommendationList.get(3);
        assertSoftly(softly -> {
            var clonedAnchor = (TestdataShadowingChainedAnchor) fourthRecommendation.proposition();
            softly.assertThat(clonedAnchor).isNotEqualTo(c0);
            softly.assertThat(clonedAnchor.getCode()).isEqualTo(c0.getCode());
            softly.assertThat(clonedAnchor.getNextEntity().getCode()).isEqualTo(c1.getCode());
            softly.assertThat(fourthRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-651));
        });

        // Ensure the original solution is in its original state.
        assertSoftly(softly -> {
            softly.assertThat(uninitializedEntity.getNextEntity()).isNull();
            softly.assertThat(solution.getScore()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitTwoUninitializedEntityWithChained(SolutionManagerSource SolutionManagerSource) {
        var a0 = new TestdataShadowingChainedAnchor("a0");
        var b0 = new TestdataShadowingChainedAnchor("b0");
        var b1 = new TestdataShadowingChainedEntity("b1", b0);
        var c0 = new TestdataShadowingChainedAnchor("c0");
        var c1 = new TestdataShadowingChainedEntity("c1", c0);
        var c2 = new TestdataShadowingChainedEntity("c2", c1);
        var uninitializedEntity = new TestdataShadowingChainedEntity("uninitialized");
        var uninitializedEntity2 = new TestdataShadowingChainedEntity("uninitialized2");
        var uninitializedSolution = new TestdataShadowingChainedSolution("solution");
        uninitializedSolution.setChainedAnchorList(Arrays.asList(a0, b0, c0));
        uninitializedSolution.setChainedEntityList(Arrays.asList(b1, c1, c2, uninitializedEntity, uninitializedEntity2));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_CHAINED);
        assertThatThrownBy(() -> solutionManager.recommendFit(uninitializedSolution, uninitializedEntity,
                TestdataShadowingChainedEntity::getChainedObject))
                .hasMessageContaining("Solution (solution) has (2) uninitialized elements.");
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitList(SolutionManagerSource SolutionManagerSource) {
        var a = new TestdataListEntityWithShadowHistory("a");
        var b0 = new TestdataListValueWithShadowHistory("b0");
        var b = new TestdataListEntityWithShadowHistory("b", b0);
        var c0 = new TestdataListValueWithShadowHistory("c0");
        var c1 = new TestdataListValueWithShadowHistory("c1");
        var c = new TestdataListEntityWithShadowHistory("c", c0, c1);
        var solution = new TestdataListSolutionWithShadowHistory();
        TestdataListValueWithShadowHistory uninitializedValue = new TestdataListValueWithShadowHistory("uninitialized");
        solution.setEntityList(Arrays.asList(a, b, c));
        solution.setValueList(Arrays.asList(b0, c0, c1, uninitializedValue));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_LIST);
        var recommendationList =
                solutionManager.recommendFit(solution, uninitializedValue, v -> new Pair<>(v.getEntity(), v.getIndex()));
        assertThat(recommendationList).hasSize(6);

        // First recommendation is to be added to the "a" list variable, as that results in the shortest list.
        var firstRecommendation = recommendationList.get(0);
        assertSoftly(softly -> {
            var result = (Pair<TestdataListEntityWithShadowHistory, Integer>) firstRecommendation.proposition();
            softly.assertThat(result.value()).isEqualTo(0); // Beginning of the list.
            // The entity is cloned...
            var entity = result.key();
            softly.assertThat(entity).isNotEqualTo(a);
            softly.assertThat(entity.getCode()).isEqualTo(a.getCode());
            // ... but it is in a state as it would've been in the original solution.
            softly.assertThat(entity.getValueList()).isEmpty();
            softly.assertThat(firstRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-1));
        });

        // Second recommendation is to be added to the start of the "b" list variable.
        var secondRecommendation = recommendationList.get(1);
        assertSoftly(softly -> {
            var result = (Pair<TestdataListEntityWithShadowHistory, Integer>) secondRecommendation.proposition();
            softly.assertThat(result.value()).isEqualTo(0); // Beginning of the list.
            var entity = result.key();
            softly.assertThat(entity).isNotEqualTo(b);
            softly.assertThat(entity.getCode()).isEqualTo(b.getCode());
            softly.assertThat(entity.getValueList()).hasSize(1);
            softly.assertThat(secondRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-3));
        });

        // Third recommendation is to be added to the end of the "b" list variable.
        var thirdRecommendation = recommendationList.get(2);
        assertSoftly(softly -> {
            var result = (Pair<TestdataListEntityWithShadowHistory, Integer>) thirdRecommendation.proposition();
            softly.assertThat(result.value()).isEqualTo(1); // End of the list.
            var entity = result.key();
            softly.assertThat(entity).isNotEqualTo(b);
            softly.assertThat(entity.getCode()).isEqualTo(b.getCode());
            softly.assertThat(entity.getValueList()).hasSize(1);
            softly.assertThat(thirdRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-3));
        });

        // Fourth recommendation is to be added to the "c" list variable and so on...
        var fourthRecommendation = recommendationList.get(3);
        assertSoftly(softly -> {
            var result = (Pair<TestdataListEntityWithShadowHistory, Integer>) fourthRecommendation.proposition();
            softly.assertThat(result.value()).isEqualTo(0); // Beginning of the list.
            var entity = result.key();
            softly.assertThat(entity.getCode()).isNotEqualTo(c);
            softly.assertThat(entity.getCode()).isEqualTo(c.getCode());
            softly.assertThat(entity.getValueList()).hasSize(2);
            softly.assertThat(fourthRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-5));
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitTwoUninitializedEntityWithList(SolutionManagerSource SolutionManagerSource) {
        var a = new TestdataListEntityWithShadowHistory("a");
        var b0 = new TestdataListValueWithShadowHistory("b0");
        var b = new TestdataListEntityWithShadowHistory("b", b0);
        var c0 = new TestdataListValueWithShadowHistory("c0");
        var c1 = new TestdataListValueWithShadowHistory("c1");
        var c = new TestdataListEntityWithShadowHistory("c", c0, c1);
        var d = new TestdataListEntityWithShadowHistory("d");
        var solution = new TestdataListSolutionWithShadowHistory();
        TestdataListValueWithShadowHistory uninitializedValue = new TestdataListValueWithShadowHistory("uninitialized");
        solution.setEntityList(Arrays.asList(a, b, c, d));
        solution.setValueList(Arrays.asList(b0, c0, c1, uninitializedValue));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_LIST);
        var recommendationList =
                solutionManager.recommendFit(solution, uninitializedValue, v -> new Pair<>(v.getEntity(), v.getIndex()));
        assertThat(recommendationList).hasSize(7);
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitListPinned(SolutionManagerSource SolutionManagerSource) {
        var a = new TestdataPinnedWithIndexListEntity("a");
        var b0 = new TestdataPinnedWithIndexListValue("b0");
        var b = new TestdataPinnedWithIndexListEntity("b", b0);
        b.setPinned(true); // Entity will be unavailable.
        var c0 = new TestdataPinnedWithIndexListValue("c0");
        var c1 = new TestdataPinnedWithIndexListValue("c1");
        var c = new TestdataPinnedWithIndexListEntity("c", c0, c1);
        c.setPinned(false);
        c.setPlanningPinToIndex(1); // Destination c[0] will be unavailable.
        var solution = new TestdataPinnedWithIndexListSolution();
        var uninitializedValue = new TestdataPinnedWithIndexListValue("uninitialized");
        solution.setEntityList(Arrays.asList(a, b, c));
        solution.setValueList(Arrays.asList(b0, c0, c1, uninitializedValue));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_LIST_PINNED);
        var recommendationList =
                solutionManager.recommendFit(solution, uninitializedValue,
                        v -> new Pair<>(v.getEntity(), v.getEntity().getValueList().indexOf(v)));
        assertThat(recommendationList).hasSize(3);

        // First recommendation is to be added to the "a" list variable, as that results in the shortest list.
        var firstRecommendation = recommendationList.get(0);
        assertSoftly(softly -> {
            var result = (Pair<TestdataPinnedWithIndexListEntity, Integer>) firstRecommendation.proposition();
            softly.assertThat(result.value()).isEqualTo(0); // Beginning of the list.
            // The entity is cloned...
            var entity = result.key();
            softly.assertThat(entity).isNotEqualTo(a);
            softly.assertThat(entity.getCode()).isEqualTo(a.getCode());
            // ... but it is in a state as it would've been in the original solution.
            softly.assertThat(entity.getValueList()).isEmpty();
            softly.assertThat(firstRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-1));
        });

        // Second recommendation is to be added to c[1].
        var secondRecommendation = recommendationList.get(1);
        assertSoftly(softly -> {
            var result = (Pair<TestdataPinnedWithIndexListEntity, Integer>) secondRecommendation.proposition();
            softly.assertThat(result.value()).isEqualTo(1); // First unpinned index.
            var entity = result.key();
            softly.assertThat(entity).isNotEqualTo(c);
            softly.assertThat(entity.getCode()).isEqualTo(c.getCode());
            softly.assertThat(entity.getValueList()).hasSize(2);
            softly.assertThat(secondRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-5));
        });

        // Third recommendation is to be added to c[2].
        var thirdRecommendation = recommendationList.get(2);
        assertSoftly(softly -> {
            var result = (Pair<TestdataPinnedWithIndexListEntity, Integer>) thirdRecommendation.proposition();
            softly.assertThat(result.value()).isEqualTo(2); // End of the list.
            var entity = result.key();
            softly.assertThat(entity).isNotEqualTo(c);
            softly.assertThat(entity.getCode()).isEqualTo(c.getCode());
            softly.assertThat(entity.getValueList()).hasSize(2);
            softly.assertThat(thirdRecommendation.scoreAnalysisDiff().score()).isEqualTo(SimpleScore.of(-5));
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendFitTwoUninitializedEntityWithListPinned(SolutionManagerSource SolutionManagerSource) {
        var a = new TestdataPinnedWithIndexListEntity("a");
        var b0 = new TestdataPinnedWithIndexListValue("b0");
        var b = new TestdataPinnedWithIndexListEntity("b", b0);
        b.setPinned(true); // Entity will be unavailable.
        var c0 = new TestdataPinnedWithIndexListValue("c0");
        var c1 = new TestdataPinnedWithIndexListValue("c1");
        var c = new TestdataPinnedWithIndexListEntity("c", c0, c1);
        var d = new TestdataPinnedWithIndexListEntity("d");
        c.setPinned(false);
        c.setPlanningPinToIndex(1); // Destination c[0] will be unavailable.
        var solution = new TestdataPinnedWithIndexListSolution();
        var uninitializedValue = new TestdataPinnedWithIndexListValue("uninitialized");
        solution.setEntityList(Arrays.asList(a, b, c, d));
        solution.setValueList(Arrays.asList(b0, c0, c1, uninitializedValue));

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_LIST_PINNED);
        var recommendationList =
                solutionManager.recommendFit(solution, uninitializedValue,
                        v -> new Pair<>(v.getEntity(), v.getEntity().getValueList().indexOf(v)));
        assertThat(recommendationList).hasSize(4);
    }

    public enum SolutionManagerSource {

        FROM_SOLVER_FACTORY(SolutionManager::create),
        FROM_SOLVER_MANAGER(solverFactory -> SolutionManager.create(SolverManager.create(solverFactory)));

        private final Function<SolverFactory, SolutionManager> solutionManagerConstructor;

        SolutionManagerSource(Function<SolverFactory, SolutionManager> SolutionManagerConstructor) {
            this.solutionManagerConstructor = SolutionManagerConstructor;
        }

        public <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_>
                createSolutionManager(SolverFactory<Solution_> solverFactory) {
            return solutionManagerConstructor.apply(solverFactory);
        }

    }

}