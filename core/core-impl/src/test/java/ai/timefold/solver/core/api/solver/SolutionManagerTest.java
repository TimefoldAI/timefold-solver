package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListEntityWithShadowHistory;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListSolutionWithShadowHistory;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListValueWithShadowHistory;
import ai.timefold.solver.core.impl.testdata.domain.nullable.TestdataNullableSolution;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class SolutionManagerTest {

    public static final SolverFactory<TestdataShadowedSolution> SOLVER_FACTORY =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataShadowedSolverConfig.xml");
    public static final SolverFactory<TestdataShadowingChainedSolution> SOLVER_FACTORY_CHAINED =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataShadowingChainedSolverConfig.xml");
    public static final SolverFactory<TestdataListSolutionWithShadowHistory> SOLVER_FACTORY_LIST =
            SolverFactory
                    .createFromXmlResource("ai/timefold/solver/core/api/solver/testdataListWithShadowHistorySolverConfig.xml");
    public static final SolverFactory<TestdataNullableSolution> SOLVER_FACTORY_OVERCONSTRAINED =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataOverconstrainedSolverConfig.xml");

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverything(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
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

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
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

        var solutionManager = SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
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

        assertThatThrownBy(() -> solutionManager.analyze(solution))
                .hasMessageContaining("not initialized");
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void analyzeNullableWithNullValue(SolutionManagerSource SolutionManagerSource) {
        var solution = TestdataNullableSolution.generateSolution();
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
