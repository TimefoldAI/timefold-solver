package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListEntityWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListSolutionWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListValueWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListWithShadowHistoryConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.TestdataShadowedConstraintProviderClass;
import ai.timefold.solver.core.testdomain.shadow.TestdataShadowedEntity;
import ai.timefold.solver.core.testdomain.shadow.TestdataShadowedSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class SolutionManagerTest {

    public static final SolverFactory<TestdataShadowedSolution> SOLVER_FACTORY_SHADOWED = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataShadowedSolution.class)
                    .withEntityClasses(TestdataShadowedEntity.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig()
                                    .withConstraintProviderClass(TestdataShadowedConstraintProviderClass.class)));
    public static final SolverFactory<TestdataConcurrentSolution> SOLVER_FACTORY_DECLARATIVE_SHADOW = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataConcurrentSolution.class)
                    .withEntityClasses(TestdataConcurrentEntity.class, TestdataConcurrentValue.class)
                    .withConstraintProviderClass(TestdataConcurrentConstraintProvider.class));
    public static final SolverFactory<TestdataListSolutionWithShadowHistory> SOLVER_FACTORY_LIST = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataListSolutionWithShadowHistory.class)
                    .withEntityClasses(TestdataListEntityWithShadowHistory.class, TestdataListValueWithShadowHistory.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig()
                                    .withConstraintProviderClass(TestdataListWithShadowHistoryConstraintProvider.class)));
    public static final SolverFactory<TestdataSolution> SOLVER_FACTORY_WITH_CS = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataSolution.class)
                    .withEntityClasses(TestdataEntity.class)
                    .withConstraintProviderClass(TestdataConstraintProvider.class));
    public static final SolverFactory<TestdataSolution> SOLVER_FACTORY_EASY = SolverFactory.create(
            new SolverConfig()
                    .withSolutionClass(TestdataSolution.class)
                    .withEntityClasses(TestdataEntity.class)
                    .withScoreDirectorFactory(
                            new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)));

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverything(SolutionManagerSource solutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().getFirst().getFirstShadow()).isNull();
        });

        var solutionManager = solutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            softly.assertThat(solution.getEntityList().getFirst().getFirstShadow()).isNotNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateEverythingList(SolutionManagerSource solutionManagerSource) {
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

        var solutionManager = solutionManagerSource.createSolutionManager(SOLVER_FACTORY_LIST);
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
    void updateOnlyShadowVariables(SolutionManagerSource solutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().getFirst().getFirstShadow()).isNull();
        });

        var solutionManager = solutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SHADOW_VARIABLES_ONLY);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().getFirst().getFirstShadow()).isNotNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateOnlyScore(SolutionManagerSource solutionManagerSource) {
        var solution = TestdataShadowedSolution.generateSolution();

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNull();
            softly.assertThat(solution.getEntityList().getFirst().getFirstShadow()).isNull();
        });

        var solutionManager = solutionManagerSource.createSolutionManager(SOLVER_FACTORY_SHADOWED);
        assertThat(solutionManager).isNotNull();
        solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);

        assertSoftly(softly -> {
            softly.assertThat(solution.getScore()).isNotNull();
            softly.assertThat(solution.getEntityList().getFirst().getFirstShadow()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateOnlyScoreDeclarativeShadows(SolutionManagerSource solutionManagerSource) {
        var solution = new TestdataConcurrentSolution();
        var e1 = new TestdataConcurrentEntity("e1");
        var e2 = new TestdataConcurrentEntity("e2");

        var a1 = new TestdataConcurrentValue("a1");
        var a2 = new TestdataConcurrentValue("a2");
        var b1 = new TestdataConcurrentValue("b1");
        var b2 = new TestdataConcurrentValue("b2");

        var groupA = List.of(a1, a2);
        var groupB = List.of(b1, b2);

        var entities = List.of(e1, e2);
        var values = List.of(a1, a2, b1, b2);

        a1.setConcurrentValueGroup(groupA);
        a2.setConcurrentValueGroup(groupA);

        b1.setConcurrentValueGroup(groupB);
        b2.setConcurrentValueGroup(groupB);

        e1.setValues(List.of(a1, b1));
        e2.setValues(List.of(b2, a2));

        b1.setPreviousValue(a1);
        a2.setPreviousValue(b2);

        a1.setNextValue(b1);
        b2.setNextValue(a2);

        a1.setIndex(0);
        a2.setIndex(1);

        b1.setIndex(1);
        b2.setIndex(0);

        a1.setEntity(e1);
        b1.setEntity(e1);
        a2.setEntity(e2);
        b2.setEntity(e2);

        a1.setInconsistent(true);
        a2.setInconsistent(true);
        b1.setInconsistent(true);
        b2.setInconsistent(true);

        solution.setEntities(entities);
        solution.setValues(values);

        var solutionManager = solutionManagerSource.createSolutionManager(SOLVER_FACTORY_DECLARATIVE_SHADOW);
        var score = solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);
        assertThat(score).isEqualTo(HardSoftScore.ofHard(-4));
        assertThat(solution.getScore()).isEqualTo(HardSoftScore.ofHard(-4));
    }

    @Test
    void updateShadowVariableFailsIfReferencedEntitiesAreNotGiven() {
        var e1 = new TestdataConcurrentEntity("e1");
        var e2 = new TestdataConcurrentEntity("e2");

        var a1 = new TestdataConcurrentValue("a1");
        var a2 = new TestdataConcurrentValue("a2");
        var b1 = new TestdataConcurrentValue("b1");
        var b2 = new TestdataConcurrentValue("b2");

        var groupA = List.of(a1, a2);
        var groupB = List.of(b1, b2);

        a1.setConcurrentValueGroup(groupA);
        a2.setConcurrentValueGroup(groupA);

        b1.setConcurrentValueGroup(groupB);
        b2.setConcurrentValueGroup(groupB);

        e1.setValues(List.of(a1, b1));
        e2.setValues(List.of(b2, a2));

        b1.setPreviousValue(a1);
        a2.setPreviousValue(b2);

        a1.setNextValue(b1);
        b2.setNextValue(a2);

        a1.setEntity(e1);
        b1.setEntity(e1);
        a2.setEntity(e2);
        b2.setEntity(e2);

        assertThatCode(() -> SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, a1, e1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(
                        "The entity's (%s) shadow variable (serviceStartTime) refers to a declarative shadow variable on a non-given entity (%s)"
                                .formatted(a1,
                                        a2),
                        "The entity's (%s) shadow variable (serviceReadyTime) refers to a declarative shadow variable on a non-given entity (%s)"
                                .formatted(a2,
                                        b2),
                        "The entity's (%s) shadow variable (serviceStartTime) refers to a declarative shadow variable on a non-given entity (%s)"
                                .formatted(b2, b1));
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void updateOnlyScoreFailsIfListVariableInconsistent(SolutionManagerSource solutionManagerSource) {
        var solution = new TestdataConcurrentSolution();
        var e1 = new TestdataConcurrentEntity("e1");
        var e2 = new TestdataConcurrentEntity("e2");

        var a1 = new TestdataConcurrentValue("a1");
        var a2 = new TestdataConcurrentValue("a2");
        var b1 = new TestdataConcurrentValue("b1");
        var b2 = new TestdataConcurrentValue("b2");

        var groupA = List.of(a1, a2);
        var groupB = List.of(b1, b2);

        var entities = List.of(e1, e2);
        var values = List.of(a1, a2, b1, b2);

        a1.setConcurrentValueGroup(groupA);
        a2.setConcurrentValueGroup(groupA);

        b1.setConcurrentValueGroup(groupB);
        b2.setConcurrentValueGroup(groupB);

        e1.setValues(List.of(a1, b1));
        e2.setValues(List.of(b2, a2));

        b1.setPreviousValue(a1);
        a2.setPreviousValue(b2);

        a1.setNextValue(b1);
        b2.setNextValue(a2);

        a1.setIndex(0);
        a2.setIndex(1);

        b1.setIndex(1);
        b2.setIndex(0);

        a1.setEntity(e1);
        b1.setEntity(e2);
        a2.setEntity(e2);
        b2.setEntity(e2);

        a1.setInconsistent(true);
        a2.setInconsistent(true);
        b1.setInconsistent(true);
        b2.setInconsistent(true);

        solution.setEntities(entities);
        solution.setValues(values);

        var solutionManager = solutionManagerSource.createSolutionManager(SOLVER_FACTORY_DECLARATIVE_SHADOW);
        assertThat(solutionManager).isNotNull();
        assertThatCode(() -> solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY)).hasMessageContainingAll(
                "The entity (e1)" +
                        " has a list variable (values)" +
                        " and one of its elements (e1 -> a1 -> b1)" +
                        " which has a shadow variable (entity)" +
                        " has an oldInverseEntity (e2) which is not that entity.");
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void visualizeNodeNetwork(SolutionManagerSource solutionManagerSource) {
        var solution = new TestdataSolution();
        var solutionManager = (DefaultSolutionManager<TestdataSolution, SimpleScore>) solutionManagerSource
                .createSolutionManager(SOLVER_FACTORY_WITH_CS);
        var result = solutionManager.visualizeNodeNetwork(solution);
        assertThat(result).isEqualToIgnoringWhitespace(
                """
                        digraph {
                            rankdir=LR;
                            label=<<B>Bavet Node Network for 'null'</B><BR />1 constraints, 1 nodes>;
                            node0 -> impact0;
                            node0 [pad="0.2", fillcolor="#3e00ff", shape="plaintext", fontcolor="white", style="filled", label=<<B>ForEachFilteredUni</B><BR/>(TestdataEntity)>, fontname="Courier New"];
                            impact0 [pad="0.2", fillcolor="#3423a6", shape="plaintext", fontcolor="white", style="filled", label=<<B>Always penalize</B><BR />(Weight: -1)>, fontname="Courier New"];
                            { rank=same; node0; }
                        }""");
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void visualizeNodeNetworkNoBavet(SolutionManagerSource solutionManagerSource) {
        var solution = new TestdataSolution();
        var solutionManager = (DefaultSolutionManager<TestdataSolution, SimpleScore>) solutionManagerSource
                .createSolutionManager(SOLVER_FACTORY_EASY);
        assertThatThrownBy(() -> solutionManager.visualizeNodeNetwork(solution))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Constraint Streams");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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