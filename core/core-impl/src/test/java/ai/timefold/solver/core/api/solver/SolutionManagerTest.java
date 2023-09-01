package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.shadow.TestdataShadowedSolution;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class SolutionManagerTest {

    public static final SolverFactory<TestdataShadowedSolution> SOLVER_FACTORY =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataShadowedSolverConfig.xml");
    public static final SolverFactory<TestdataShadowingChainedSolution> SOLVER_FACTORY_CHAINED =
            SolverFactory.createFromXmlResource("ai/timefold/solver/core/api/solver/testdataShadowingChainedSolverConfig.xml");

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverything(SolutionManagerSource SolutionManagerSource) {
        SolutionManager<TestdataShadowedSolution, ?> SolutionManager =
                SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(SolutionManager).isNotNull();
        TestdataShadowedSolution solution = TestdataShadowedSolution.generateSolution();
        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });
        SolutionManager.update(solution);
        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNotNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverythingChained(SolutionManagerSource SolutionManagerSource) {
        SolutionManager<TestdataShadowingChainedSolution, ?> SolutionManager =
                SolutionManagerSource.createSolutionManager(SOLVER_FACTORY_CHAINED);
        assertThat(SolutionManager).isNotNull();

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

        SolutionManager.update(solution);
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
    void updateOnlyShadowVariables(SolutionManagerSource SolutionManagerSource) {
        SolutionManager<TestdataShadowedSolution, ?> SolutionManager =
                SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(SolutionManager).isNotNull();
        TestdataShadowedSolution solution = TestdataShadowedSolution.generateSolution();
        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });
        SolutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SHADOW_VARIABLES_ONLY);
        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNotNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateOnlyScore(SolutionManagerSource SolutionManagerSource) {
        SolutionManager<TestdataShadowedSolution, ?> SolutionManager =
                SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(SolutionManager).isNotNull();
        TestdataShadowedSolution solution = TestdataShadowedSolution.generateSolution();
        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });
        SolutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);
        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            softly.assertThat(solution.getEntityList().get(0).getFirstShadow()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void explain(SolutionManagerSource SolutionManagerSource) {
        SolutionManager<TestdataShadowedSolution, ?> solutionManager =
                SolutionManagerSource.createSolutionManager(SOLVER_FACTORY);
        assertThat(solutionManager).isNotNull();
        TestdataShadowedSolution solution = TestdataShadowedSolution.generateSolution();
        ScoreExplanation<TestdataShadowedSolution, ?> scoreExplanation = solutionManager.explain(solution);
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
