package ai.timefold.solver.core.impl.score.stream.common.uni;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.count;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countDistinct;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.max;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.min;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.toSet;
import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamFunctionalTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.testdata.domain.TestdataConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.impl.testdata.domain.extended.TestdataUnannotatedExtendedEntity;
import ai.timefold.solver.core.impl.testdata.domain.extended.TestdataUnannotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows.TestdataPinnedNoShadowsListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows.TestdataPinnedNoShadowsListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows.TestdataPinnedNoShadowsListValue;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataSimpleBigDecimalScoreSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataSimpleLongScoreSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishExtra;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishValue;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishValueGroup;

import org.junit.jupiter.api.TestTemplate;

public abstract class AbstractUniConstraintStreamTest
        extends AbstractConstraintStreamTest
        implements ConstraintStreamFunctionalTest {

    protected AbstractUniConstraintStreamTest(ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @TestTemplate
    public void filter_problemFact() {
        var solution = TestdataLavishSolution.generateSolution();
        var valueGroup1 = new TestdataLavishValueGroup("MyValueGroup 1");
        solution.getValueGroupList().add(valueGroup1);
        var valueGroup2 = new TestdataLavishValueGroup("MyValueGroup 2");
        solution.getValueGroupList().add(valueGroup2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                        .filter(valueGroup -> valueGroup.getCode().startsWith("MyValueGroup"))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(valueGroup1),
                assertMatch(valueGroup2));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(valueGroup1);
        valueGroup1.setCode("Other code");
        scoreDirector.afterProblemPropertyChanged(valueGroup1);
        assertScore(scoreDirector,
                assertMatch(valueGroup2));
    }

    @Override
    @TestTemplate
    public void filter_entity() {
        var solution = TestdataLavishSolution.generateSolution();
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() == entityGroup)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));

        // Incrementally update
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity3));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity3));
    }

    @Override
    @TestTemplate
    public void filter_consecutive() {
        var solution = TestdataLavishSolution.generateSolution(4, 4);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var entity4 = solution.getEntityList().get(3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> !Objects.equals(entity, entity1))
                        .filter(entity -> !Objects.equals(entity, entity2))
                        .filter(entity -> !Objects.equals(entity, entity3))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector, assertMatch(entity4));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity4);
        solution.getEntityList().remove(entity4);
        scoreDirector.afterEntityRemoved(entity4);
        assertScore(scoreDirector);
    }

    @TestTemplate
    public void join_unknownClass() {
        assertThatThrownBy(() -> buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                .join(Integer.class)
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Integer.class.getCanonicalName())
                .hasMessageContaining("assignable from");
    }

    @Override
    @TestTemplate
    public void join_0() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 1, 1);
        var valueGroup = new TestdataLavishValueGroup("MyValueGroup");
        solution.getValueGroupList().add(valueGroup);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                        .join(TestdataLavishEntityGroup.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstValueGroup(), solution.getFirstEntityGroup()),
                assertMatch(solution.getFirstValueGroup(), entityGroup),
                assertMatch(valueGroup, solution.getFirstEntityGroup()),
                assertMatch(valueGroup, entityGroup));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstValueGroup(), solution.getFirstEntityGroup()),
                assertMatch(valueGroup, solution.getFirstEntityGroup()));
    }

    @Override
    @TestTemplate
    public void join_1Equal() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value1);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class,
                                equal(TestdataLavishEntity::getValue))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity1),
                assertMatch(entity1, entity3),
                assertMatch(entity2, entity2),
                assertMatch(entity3, entity1),
                assertMatch(entity3, entity3));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1),
                assertMatch(entity2, entity2),
                assertMatch(entity2, entity3),
                assertMatch(entity3, entity2),
                assertMatch(entity3, entity3));

        // Incremental for which the first change matches a join that doesn't survive the second change
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value2);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value1);
        scoreDirector.afterVariableChanged(entity3, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1),
                assertMatch(entity2, entity2),
                assertMatch(entity1, entity2),
                assertMatch(entity2, entity1),
                assertMatch(entity3, entity3));
    }

    /**
     * A join must not presume that left inserts/retracts always happen before right inserts/retracts,
     * if node sharing is active.
     * This test triggers a right insert/retract before a left insert/retract.
     */
    @TestTemplate
    public void join_1_mirrored() {
        var solution = TestdataLavishSolution.generateSolution(1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        solution.getValueList().add(value2);
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(), value2);
        solution.getEntityList().add(entity2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataLavishSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        // A.join(B)
                        factory.forEach(TestdataLavishEntity.class)
                                .join(TestdataLavishValue.class,
                                        equal(TestdataLavishEntity::getValue, value -> value))
                                .penalize(SimpleScore.ONE)
                                .asConstraint("testConstraint1"),
                        // B.join(A)
                        factory.forEach(TestdataLavishValue.class)
                                .join(TestdataLavishEntity.class,
                                        equal(value -> value, TestdataLavishEntity::getValue))
                                .penalize(SimpleScore.ONE)
                                .asConstraint("testConstraint2")
                });

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch("testConstraint1", entity1, value1),
                assertMatch("testConstraint2", value1, entity1),
                assertMatch("testConstraint1", entity2, value2),
                assertMatch("testConstraint2", value2, entity2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value1);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch("testConstraint1", entity1, value1),
                assertMatch("testConstraint2", value1, entity1),
                assertMatch("testConstraint1", entity2, value1),
                assertMatch("testConstraint2", value1, entity2));
    }

    @Override
    @TestTemplate
    public void join_2Equal() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        entity1.setIntegerProperty(7);
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup, solution.getFirstValue());
        entity2.setIntegerProperty(7);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", entityGroup, solution.getFirstValue());
        entity3.setIntegerProperty(8);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class,
                                equal(TestdataLavishEntity::getEntityGroup),
                                equal(TestdataLavishEntity::getIntegerProperty))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), solution.getFirstEntity()),
                assertMatch(entity1, entity1),
                assertMatch(entity1, entity2),
                assertMatch(entity2, entity1),
                assertMatch(entity2, entity2),
                assertMatch(entity3, entity3));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity1);
        entity1.setIntegerProperty(8);
        scoreDirector.afterProblemPropertyChanged(entity1);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), solution.getFirstEntity()),
                assertMatch(entity1, entity1),
                assertMatch(entity1, entity3),
                assertMatch(entity2, entity2),
                assertMatch(entity3, entity1),
                assertMatch(entity3, entity3));
    }

    @Override
    @TestTemplate
    public void joinAfterGroupBy() {
        var solution = TestdataLavishSolution.generateSolution(1, 0, 1, 0);
        var value1 = new TestdataLavishValue("MyValue 1", solution.getFirstValueGroup());
        solution.getValueList().add(value1);
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        solution.getValueList().add(value2);
        var entity1 = new TestdataLavishEntity("MyEntity 1", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity2);
        var extra1 = new TestdataLavishExtra("MyExtra 1");
        solution.getExtraList().add(extra1);
        var extra2 = new TestdataLavishExtra("MyExtra 2");
        solution.getExtraList().add(extra2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(countDistinct(TestdataLavishEntity::getValue))
                        .join(TestdataLavishExtra.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(1, extra1),
                assertMatch(1, extra2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value2);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(2, extra1),
                assertMatch(2, extra2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity2);
        solution.getEntityList().remove(entity2);
        scoreDirector.afterEntityRemoved(entity2);
        assertScore(scoreDirector,
                assertMatch(1, extra1),
                assertMatch(1, extra2));
    }

    @Override
    @TestTemplate
    public void ifExists_unknownClass() {
        assertThatThrownBy(() -> buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                .ifExists(Integer.class)
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Integer.class.getCanonicalName())
                .hasMessageContaining("assignable from");
    }

    @Override
    @TestTemplate
    public void ifExists_0Joiner0Filter() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 1, 1);
        var valueGroup = new TestdataLavishValueGroup("MyValueGroup");
        solution.getValueGroupList().add(valueGroup);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                        .ifExists(TestdataLavishEntityGroup.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstValueGroup()),
                assertMatch(valueGroup));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstValueGroup()),
                assertMatch(valueGroup));
    }

    @Override
    @TestTemplate
    public void ifExists_0Join1Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifExists(TestdataLavishEntityGroup.class,
                                filtering((entity, group) -> Objects.equals(entity.getEntityGroup(), group)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity1),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity2));
    }

    @Override
    @TestTemplate
    public void ifExists_1Join0Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector = buildScoreDirector(factory -> factory
                .forEach(TestdataLavishEntity.class)
                .ifExists(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, Function.identity()))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity1),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity2));
    }

    @Override
    @TestTemplate
    public void ifExists_1Join1Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifExists(TestdataLavishEntityGroup.class,
                                equal(TestdataLavishEntity::getEntityGroup, Function.identity()),
                                filtering((entity, group) -> entity.getCode().contains("MyEntity")
                                        || group.getCode().contains("MyEntity")))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(entity2));
    }

    @TestTemplate
    public void ifExistsOther_1Join0Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifExistsOther(TestdataLavishEntity.class, equal(TestdataLavishEntity::getEntityGroup))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));
    }

    @Override
    @TestTemplate
    public void ifExistsDoesNotIncludeUnassigned() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("Entity with null var", solution.getFirstEntityGroup(), null);
        solution.getEntityList().add(entity3);

        // both forEach() and ifExists() will skip entity3, as it is not initialized.
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifExistsOther(TestdataLavishEntity.class, equal(TestdataLavishEntity::getEntityGroup))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));
    }

    @Override
    @TestTemplate
    @Deprecated(forRemoval = true)
    public void ifExistsIncludesNullVarsWithFrom() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("Entity with null var", solution.getFirstEntityGroup(), null);
        solution.getEntityList().add(entity3);

        // from() will skip entity3, as it is not initialized.
        // ifExists() will still catch it, as it ignores that check.
        var scoreDirector =
                buildScoreDirector(factory -> factory.from(TestdataLavishEntity.class)
                        .ifExistsOther(TestdataLavishEntity.class, equal(TestdataLavishEntity::getEntityGroup))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity1),
                assertMatch(entity2));
    }

    @Override
    @TestTemplate
    public void ifNotExists_unknownClass() {
        assertThatThrownBy(() -> buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                .ifNotExists(Integer.class)
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Integer.class.getCanonicalName())
                .hasMessageContaining("assignable from");
    }

    @Override
    @TestTemplate
    public void ifNotExists_0Joiner0Filter() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 1, 1);
        var valueGroup = new TestdataLavishValueGroup("MyValueGroup");
        solution.getValueGroupList().add(valueGroup);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                        .ifNotExists(TestdataLavishEntityGroup.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void ifNotExists_0Join1Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifNotExists(TestdataLavishEntityGroup.class,
                                filtering((entity, group) -> Objects.equals(entity.getEntityGroup(), group)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(entity1));
    }

    @Override
    @TestTemplate
    public void ifNotExists_1Join0Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifNotExists(TestdataLavishEntityGroup.class,
                                equal(TestdataLavishEntity::getEntityGroup, Function.identity()))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(entity1));
    }

    @Override
    @TestTemplate
    public void ifNotExists_1Join1Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifNotExists(TestdataLavishEntityGroup.class,
                                equal(TestdataLavishEntity::getEntityGroup, Function.identity()),
                                filtering((entity, group) -> entity.getCode().contains("MyEntity")
                                        || group.getCode().contains("MyEntity")))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity1));
    }

    @Override
    @TestTemplate
    public void ifNotExistsDoesNotIncludeUnassigned() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("Entity with null var", solution.getFirstEntityGroup(), null);
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifNotExistsOther(TestdataLavishEntity.class, equal(TestdataLavishEntity::getEntityGroup))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity1));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()),
                assertMatch(entity1));
    }

    @Override
    @TestTemplate
    @Deprecated(forRemoval = true)
    public void ifNotExistsIncludesNullVarsWithFrom() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("Entity with null var", solution.getFirstEntityGroup(), null);
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.from(TestdataLavishEntity.class)
                        .ifNotExistsOther(TestdataLavishEntity.class, equal(TestdataLavishEntity::getEntityGroup))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()));
    }

    @TestTemplate
    public void ifNotExistsOther_1Join0Filter() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .ifNotExistsOther(TestdataLavishEntity.class, equal(TestdataLavishEntity::getEntityGroup))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity()));
    }

    @Override
    @TestTemplate
    public void ifExistsAfterGroupBy() {
        var solution = TestdataLavishSolution.generateSolution(1, 0, 1, 0);
        var value1 = new TestdataLavishValue("MyValue 1", solution.getFirstValueGroup());
        solution.getValueList().add(value1);
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        solution.getValueList().add(value2);
        var entity1 = new TestdataLavishEntity("MyEntity 1", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity2);
        var extra1 = new TestdataLavishExtra("MyExtra 1");
        solution.getExtraList().add(extra1);
        var extra2 = new TestdataLavishExtra("MyExtra 2");
        solution.getExtraList().add(extra2);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(countDistinct(TestdataLavishEntity::getValue))
                        .ifExists(TestdataLavishExtra.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value2);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity2);
        solution.getEntityList().remove(entity2);
        scoreDirector.afterEntityRemoved(entity2);
        assertScore(scoreDirector,
                assertMatch(1));
    }

    @TestTemplate
    public void forEach_unknownClass() {
        assertThatThrownBy(() -> buildScoreDirector(factory -> factory.forEach(Integer.class)
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Integer.class.getCanonicalName())
                .hasMessageContaining("assignable from");
    }

    @TestTemplate
    public void forEach_polymorphism() {
        TestdataSolution solution = new TestdataUnannotatedExtendedSolution();
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        solution.setValueList(List.of(v1, v2));
        var cat = new TestdataUnannotatedExtendedEntity("Cat", v1);
        var animal = new TestdataEntity("Animal", v1);
        var dog = new TestdataUnannotatedExtendedEntity("Dog", v1);
        solution.setEntityList(List.of(cat, animal, dog));

        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint("superclassConstraint"),
                        factory.forEach(TestdataUnannotatedExtendedEntity.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint("subclassConstraint")
                });

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch("superclassConstraint", cat),
                assertMatch("superclassConstraint", animal),
                assertMatch("superclassConstraint", dog),
                assertMatch("subclassConstraint", cat),
                assertMatch("subclassConstraint", dog));
    }

    @TestTemplate
    public void forEach_basicVarUninitialized() {
        var solution = new TestdataAllowsUnassignedSolution();
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        solution.setValueList(List.of(v1, v2));
        var e1 = new TestdataAllowsUnassignedEntity("e1", v1);
        var e2 = new TestdataAllowsUnassignedEntity("e2", null);
        solution.setEntityList(List.of(e1, e2));

        InnerScoreDirector<TestdataAllowsUnassignedSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataAllowsUnassignedSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataAllowsUnassignedEntity.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(e1));

        // Incremental
        scoreDirector.beforeVariableChanged(e2, "value");
        e2.setValue(v2);
        scoreDirector.afterVariableChanged(e2, "value");
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(e1),
                assertMatch(e2));

        scoreDirector.beforeVariableChanged(e1, "value");
        e1.setValue(null);
        scoreDirector.afterVariableChanged(e1, "value");
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(e2));
    }

    @TestTemplate
    public void forEach_listVarNotAllowsUnassignedValues() {
        var solution = new TestdataListSolution();
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        solution.setValueList(List.of(v1, v2));
        var e1 = new TestdataListEntity("e1", v1);
        v1.setEntity(e1);
        v1.setIndex(0);
        var e2 = new TestdataListEntity("e2");
        solution.setEntityList(List.of(e1, e2));

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataListSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataListValue.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        // v2 is not assigned, so it should not be matched
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(v1));

        // Incremental
        scoreDirector.beforeListVariableChanged(e2, "valueList", 0, 1);
        scoreDirector.beforeListVariableElementAssigned(e2, "valueList", v2);
        e2.getValueList().add(v2);
        scoreDirector.afterListVariableElementAssigned(e2, "valueList", v2);
        scoreDirector.afterListVariableChanged(e2, "valueList", 0, 1);
        assertScore(scoreDirector,
                assertMatch(v1),
                assertMatch(v2));

        scoreDirector.beforeListVariableChanged(e1, "valueList", 0, 0);
        scoreDirector.beforeListVariableElementUnassigned(e1, "valueList", v1);
        e1.getValueList().clear();
        scoreDirector.afterListVariableElementUnassigned(e1, "valueList", v1);
        scoreDirector.afterListVariableChanged(e1, "valueList", 0, 0);
        assertScore(scoreDirector,
                assertMatch(v2));
    }

    @TestTemplate
    public void forEach_listVarNotAllowsUnassignedValues_noInverseVar() {
        var solution = new TestdataPinnedNoShadowsListSolution();
        var v1 = new TestdataPinnedNoShadowsListValue("v1");
        var v2 = new TestdataPinnedNoShadowsListValue("v2");
        solution.setValueList(List.of(v1, v2));
        var e1 = new TestdataPinnedNoShadowsListEntity("e1", v1);
        v1.setIndex(0);
        var e2 = new TestdataPinnedNoShadowsListEntity("e2");
        solution.setEntityList(List.of(e1, e2));

        InnerScoreDirector<TestdataPinnedNoShadowsListSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataPinnedNoShadowsListSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataPinnedNoShadowsListValue.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        // v2 is not assigned, so it should not be matched
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(v1));

        // Incremental
        scoreDirector.beforeListVariableChanged(e2, "valueList", 0, 1);
        scoreDirector.beforeListVariableElementAssigned(e2, "valueList", v2);
        e2.getValueList().add(v2);
        scoreDirector.afterListVariableElementAssigned(e2, "valueList", v2);
        scoreDirector.afterListVariableChanged(e2, "valueList", 0, 1);
        assertScore(scoreDirector,
                assertMatch(v1),
                assertMatch(v2));

        scoreDirector.beforeListVariableChanged(e1, "valueList", 0, 0);
        scoreDirector.beforeListVariableElementUnassigned(e1, "valueList", v1);
        e1.getValueList().clear();
        scoreDirector.afterListVariableElementUnassigned(e1, "valueList", v1);
        scoreDirector.afterListVariableChanged(e1, "valueList", 0, 0);
        assertScore(scoreDirector,
                assertMatch(v2));
    }

    @TestTemplate
    public void forEach_listVarAllowsUnassignedValues() {
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        var v1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("v2");
        solution.setValueList(List.of(v1, v2));
        var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
        v1.setEntity(e1);
        v1.setIndex(0);
        var e2 = new TestdataAllowsUnassignedValuesListEntity("e2");
        solution.setEntityList(List.of(e1, e2));

        InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        // v2 is not assigned, so it should not be matched
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(v1));

        // Incremental
        scoreDirector.beforeListVariableChanged(e2, "valueList", 0, 1);
        scoreDirector.beforeListVariableElementAssigned(e2, "valueList", v2);
        e2.getValueList().add(v2);
        scoreDirector.afterListVariableElementAssigned(e2, "valueList", v2);
        scoreDirector.afterListVariableChanged(e2, "valueList", 0, 1);
        assertScore(scoreDirector,
                assertMatch(v1),
                assertMatch(v2));

        scoreDirector.beforeListVariableChanged(e1, "valueList", 0, 0);
        scoreDirector.beforeListVariableElementUnassigned(e1, "valueList", v1);
        e1.getValueList().clear();
        scoreDirector.afterListVariableElementUnassigned(e1, "valueList", v1);
        scoreDirector.afterListVariableChanged(e1, "valueList", 0, 0);
        assertScore(scoreDirector,
                assertMatch(v2));
    }

    @TestTemplate
    public void forEachIncludingUnassigned_basicVarUninitialized() {
        var solution = new TestdataAllowsUnassignedSolution();
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        solution.setValueList(List.of(v1, v2));
        var e1 = new TestdataAllowsUnassignedEntity("e1", v1);
        var e2 = new TestdataAllowsUnassignedEntity("e2", null);
        solution.setEntityList(List.of(e1, e2));

        InnerScoreDirector<TestdataAllowsUnassignedSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataAllowsUnassignedSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachIncludingUnassigned(TestdataAllowsUnassignedEntity.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(e1),
                assertMatch(e2));
    }

    @TestTemplate
    public void forEachIncludingUnassigned_listVarNotAllowsUnassignedValues() {
        var solution = new TestdataListSolution();
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        solution.setValueList(List.of(v1, v2));
        var e1 = new TestdataListEntity("e1", v1);
        var e2 = new TestdataListEntity("e2");
        solution.setEntityList(List.of(e1, e2));

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataListSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachIncludingUnassigned(TestdataListValue.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        // Even though only one value is assigned, both are matched.
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(v1),
                assertMatch(v2));

    }

    @TestTemplate
    public void forEachIncludingUnassigned_listVarAllowsUnassignedValues() {
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        var v1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("v2");
        solution.setValueList(List.of(v1, v2));
        var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
        var e2 = new TestdataAllowsUnassignedValuesListEntity("e2");
        v1.setEntity(e1);
        v1.setIndex(0);
        solution.setEntityList(List.of(e1, e2));

        InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachIncludingUnassigned(TestdataAllowsUnassignedValuesListValue.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        // Even though only one value is assigned, both are matched.
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(v1),
                assertMatch(v2));

    }

    @TestTemplate
    public void forEachUniquePair_0() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 0);
        var entityB = new TestdataLavishEntity("B", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entityB);
        var entityA = new TestdataLavishEntity("A", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entityA);
        var entityC = new TestdataLavishEntity("C", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entityC);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entityA, entityB),
                assertMatch(entityA, entityC),
                assertMatch(entityB, entityC));
    }

    @TestTemplate
    public void forEachUniquePair_1Equals() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 0);
        var entityB = new TestdataLavishEntity("B", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        entityB.setIntegerProperty(2);
        solution.getEntityList().add(entityB);
        var entityA = new TestdataLavishEntity("A", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        entityA.setIntegerProperty(2);
        solution.getEntityList().add(entityA);
        var entityC = new TestdataLavishEntity("C", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        entityC.setIntegerProperty(10);
        solution.getEntityList().add(entityC);

        var scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getIntegerProperty))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entityA, entityB));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entityB);
        entityB.setIntegerProperty(10);
        scoreDirector.afterProblemPropertyChanged(entityB);
        assertScore(scoreDirector,
                assertMatch(entityB, entityC));
    }

    @TestTemplate
    public void groupBy_1Mapping0Collect_filtered() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup)
                        .filter(entityGroup -> Objects.equals(entityGroup, entityGroup1))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector, assertMatchWithScore(-1, entityGroup1));
    }

    @TestTemplate
    public void groupBy_1Mapping1Collect_filtered() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup, count())
                        .filter((entityGroup, count) -> count > 1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, entityGroup1, 2),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), 8));
    }

    @TestTemplate
    public void groupBy_joinedAndFiltered() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup)
                        .join(TestdataLavishEntity.class, equal(Function.identity(), TestdataLavishEntity::getEntityGroup))
                        .filter((group, entity) -> group.equals(entityGroup1))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, entityGroup1, entity1),
                assertMatchWithScore(-1, entityGroup1, entity2));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping0Collector() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, solution.getFirstEntityGroup()),
                assertMatchWithScore(-1, entityGroup1));

        // Incremental
        Stream.of(entity1, entity2).forEach(entity -> {
            scoreDirector.beforeEntityRemoved(entity);
            solution.getEntityList().remove(entity);
            scoreDirector.afterEntityRemoved(entity);
        });
        assertScore(scoreDirector, assertMatchWithScore(-1, solution.getFirstEntityGroup()));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping1Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup, count())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), 2),
                assertMatchWithScore(-1, solution.getEntityGroupList().get(1), 1));

        // Incremental
        Stream.of(solution.getEntityList().get(0), solution.getEntityList().get(1))
                .forEach(entity -> {
                    scoreDirector.beforeEntityRemoved(entity);
                    solution.getEntityList().remove(entity);
                    scoreDirector.afterEntityRemoved(entity);
                });
        assertScore(scoreDirector,
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), 1));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping2Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup,
                                count(),
                                toSet())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), 2, asSet(entity1, entity3)),
                assertMatchWithScore(-1, solution.getEntityGroupList().get(1), 1, Collections.singleton(entity2)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), 1, Collections.singleton(entity3)),
                assertMatchWithScore(-1, solution.getEntityGroupList().get(1), 1, Collections.singleton(entity2)));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping3Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup,
                                count(),
                                countDistinct(),
                                toSet())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), 2, 2, asSet(entity1, entity3)),
                assertMatchWithScore(-1, solution.getEntityGroupList().get(1), 1, 1, Collections.singleton(entity2)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), 1, 1, Collections.singleton(entity3)),
                assertMatchWithScore(-1, solution.getEntityGroupList().get(1), 1, 1, Collections.singleton(entity2)));
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping1Collector() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(count())
                        .penalize(SimpleScore.ONE, count -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector, assertMatchWithScore(-10, 10));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector, assertMatchWithScore(-9, 9));
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping2Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(count(),
                                countDistinct())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector, assertMatchWithScore(-1, 3, 3));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector, assertMatchWithScore(-1, 2, 2));
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping3Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(count(),
                                min(TestdataLavishEntity::getIntegerProperty),
                                max(TestdataLavishEntity::getIntegerProperty))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        entity1.setIntegerProperty(0);
        var entity2 = solution.getEntityList().get(1);
        entity2.setIntegerProperty(1);
        var entity3 = solution.getEntityList().get(2);
        entity3.setIntegerProperty(2);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 3, 0, 2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 2, 1, 2));
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping4Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(count(),
                                min(TestdataLavishEntity::getIntegerProperty),
                                max(TestdataLavishEntity::getIntegerProperty),
                                toSet())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        entity1.setIntegerProperty(0);
        var entity2 = solution.getEntityList().get(1);
        entity2.setIntegerProperty(1);
        var entity3 = solution.getEntityList().get(2);
        entity3.setIntegerProperty(2);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 3, 0, 2, asSet(entity1, entity2, entity3)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 2, 1, 2, asSet(entity2, entity3)));
    }

    @TestTemplate
    public void groupBy_1Mapping1Collector_groupingOnPrimitives() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, solution.getFirstValue());
        entity1.setIntegerProperty(1);
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        entity2.setIntegerProperty(2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        entity3.setIntegerProperty(3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getIntegerProperty, count())
                        .penalize(SimpleScore.ONE, (integerProperty, count) -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-8, 1, 8),
                assertMatchWithScore(-1, 2, 1),
                assertMatchWithScore(-1, 3, 1));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatchWithScore(-8, 1, 8),
                assertMatchWithScore(-1, 2, 1));
    }

    @Override
    @TestTemplate
    public void groupBy_2Mapping0Collector() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var secondValue = solution.getValueList().get(1);
        var entity3 = new TestdataLavishEntity("MyEntity 3", entityGroup1, secondValue);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, entityGroup1, solution.getFirstValue()),
                assertMatchWithScore(-1, entityGroup1, secondValue),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(0)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(1)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(2)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(3)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(4)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, entityGroup1, solution.getFirstValue()),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(0)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(1)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(2)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(3)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(4)));

        // Ensure that the first match is still there when entity2, as it still has entity1
        scoreDirector.beforeEntityRemoved(entity2);
        solution.getEntityList().remove(entity2);
        scoreDirector.afterEntityRemoved(entity2);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, entityGroup1, solution.getFirstValue()),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(0)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(1)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(2)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(3)),
                assertMatchWithScore(-1, solution.getFirstEntityGroup(), solution.getValueList().get(4)));
    }

    @Override
    @TestTemplate
    public void groupBy_2Mapping1Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var value1 = new TestdataLavishValue("MyValue", solution.getFirstValueGroup());
        solution.getValueList().add(value1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, value1);
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", entityGroup1, value1);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue, count())
                        .penalize(SimpleScore.ONE, (entityGroup, value, count) -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-7, solution.getFirstEntityGroup(), solution.getFirstValue(), 7),
                assertMatchWithScore(-2, entityGroup1, value1, 2),
                assertMatchWithScore(-1, entityGroup1, solution.getFirstValue(), 1));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(solution.getFirstEntityGroup());
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatchWithScore(-8, solution.getFirstEntityGroup(), solution.getFirstValue(), 8),
                assertMatchWithScore(-2, entityGroup1, value1, 2));
    }

    @Override
    @TestTemplate
    public void groupBy_2Mapping2Collector() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 1, 7);
        var entityGroup1 = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup1);
        var value1 = new TestdataLavishValue("MyValue", solution.getFirstValueGroup());
        solution.getValueList().add(value1);
        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup1, value1);
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup1, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", entityGroup1, value1);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue, count(), count())
                        .penalize(SimpleScore.ONE, (entityGroup, value, count, sameCount) -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-7, solution.getFirstEntityGroup(), solution.getFirstValue(), 7, 7),
                assertMatchWithScore(-2, entityGroup1, value1, 2, 2),
                assertMatchWithScore(-1, entityGroup1, solution.getFirstValue(), 1, 1));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setEntityGroup(solution.getFirstEntityGroup());
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatchWithScore(-8, solution.getFirstEntityGroup(), solution.getFirstValue(), 8, 8),
                assertMatchWithScore(-2, entityGroup1, value1, 2, 2));
    }

    @Override
    @TestTemplate
    public void groupBy_3Mapping0Collector() {
        var solution = TestdataLavishSolution.generateSolution(2, 3, 2, 5);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue,
                                TestdataLavishEntity::getCode)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var entity4 = solution.getEntityList().get(3);
        var entity5 = solution.getEntityList().get(4);
        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);
        var value1 = solution.getFirstValue();
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group1, value1, entity1.getCode()),
                assertMatchWithScore(-1, group2, value2, entity2.getCode()),
                assertMatchWithScore(-1, group1, value3, entity3.getCode()),
                assertMatchWithScore(-1, group2, value1, entity4.getCode()),
                assertMatchWithScore(-1, group1, value2, entity5.getCode()));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, value2, entity2.getCode()),
                assertMatchWithScore(-1, group1, value3, entity3.getCode()),
                assertMatchWithScore(-1, group2, value1, entity4.getCode()),
                assertMatchWithScore(-1, group1, value2, entity5.getCode()));
    }

    @Override
    @TestTemplate
    public void groupBy_3Mapping1Collector() {
        var solution = TestdataLavishSolution.generateSolution(2, 3, 2, 5);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue,
                                TestdataLavishEntity::getCode, ConstraintCollectors.toSet())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var entity4 = solution.getEntityList().get(3);
        var entity5 = solution.getEntityList().get(4);
        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);
        var value1 = solution.getFirstValue();
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group1, value1, entity1.getCode(), Collections.singleton(entity1)),
                assertMatchWithScore(-1, group2, value2, entity2.getCode(), Collections.singleton(entity2)),
                assertMatchWithScore(-1, group1, value3, entity3.getCode(), Collections.singleton(entity3)),
                assertMatchWithScore(-1, group2, value1, entity4.getCode(), Collections.singleton(entity4)),
                assertMatchWithScore(-1, group1, value2, entity5.getCode(), Collections.singleton(entity5)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, value2, entity2.getCode(), Collections.singleton(entity2)),
                assertMatchWithScore(-1, group1, value3, entity3.getCode(), Collections.singleton(entity3)),
                assertMatchWithScore(-1, group2, value1, entity4.getCode(), Collections.singleton(entity4)),
                assertMatchWithScore(-1, group1, value2, entity5.getCode(), Collections.singleton(entity5)));
    }

    @Override
    @TestTemplate
    public void groupBy_4Mapping0Collector() {
        var solution = TestdataLavishSolution.generateSolution(2, 3, 2, 5);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(Function.identity(), TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue,
                                TestdataLavishEntity::getCode)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var entity4 = solution.getEntityList().get(3);
        var entity5 = solution.getEntityList().get(4);
        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);
        var value1 = solution.getFirstValue();
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, entity1, group1, value1, entity1.getCode()),
                assertMatchWithScore(-1, entity2, group2, value2, entity2.getCode()),
                assertMatchWithScore(-1, entity3, group1, value3, entity3.getCode()),
                assertMatchWithScore(-1, entity4, group2, value1, entity4.getCode()),
                assertMatchWithScore(-1, entity5, group1, value2, entity5.getCode()));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, entity2, group2, value2, entity2.getCode()),
                assertMatchWithScore(-1, entity3, group1, value3, entity3.getCode()),
                assertMatchWithScore(-1, entity4, group2, value1, entity4.getCode()),
                assertMatchWithScore(-1, entity5, group1, value2, entity5.getCode()));
    }

    @Override
    @TestTemplate
    public void distinct() { // On a distinct stream, this is a no-op.
        var solution = TestdataLavishSolution.generateSolution(2, 2, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var entity1 = solution.getFirstEntity();
        var entity2 = solution.getEntityList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));
    }

    @Override
    @TestTemplate
    public void mapToUniWithDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 1, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .map(TestdataLavishEntity::getEntityGroup) // Two entities, just one group => duplicates.
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group = solution.getFirstEntityGroup();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group),
                assertMatch(group));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group));
    }

    @Override
    @TestTemplate
    public void mapToUniWithoutDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .map(TestdataLavishEntity::getEntityGroup) // Two entities, two groups => no duplicates.
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group2));
    }

    @Override
    @TestTemplate
    public void mapToUniAndDistinctWithDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 1, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .map(TestdataLavishEntity::getEntityGroup) // Two entities, just one group => duplicates.
                        .distinct() // Duplicate copies removed here.
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group = solution.getFirstEntityGroup();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group));
    }

    @Override
    @TestTemplate
    public void mapToUniAndDistinctWithoutDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .map(TestdataLavishEntity::getEntityGroup) // Two entities, two groups => no duplicates.
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group2));
    }

    @Override
    @TestTemplate
    public void mapToBi() {
        var solution = TestdataLavishSolution.generateSolution(1, 2, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .map(TestdataLavishEntity::getEntityGroup,
                                TestdataLavishEntity::getValue)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var value1 = solution.getFirstEntity().getValue();
        var group2 = solution.getEntityGroupList().get(1);
        var value2 = solution.getEntityList().get(1).getValue();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1, value1),
                assertMatch(group2, value2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group2, value2));
    }

    @Override
    @TestTemplate
    public void mapToTri() {
        var solution = TestdataLavishSolution.generateSolution(1, 2, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .map(TestdataLavishEntity::getEntityGroup,
                                TestdataLavishEntity::getValue,
                                TestdataLavishEntity::getCode)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var value1 = solution.getFirstEntity().getValue();
        var code1 = solution.getFirstEntity().getCode();
        var group2 = solution.getEntityGroupList().get(1);
        var value2 = solution.getEntityList().get(1).getValue();
        var code2 = solution.getEntityList().get(1).getCode();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1, value1, code1),
                assertMatch(group2, value2, code2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group2, value2, code2));
    }

    @Override
    @TestTemplate
    public void mapToQuad() {
        var solution = TestdataLavishSolution.generateSolution(1, 2, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .map(TestdataLavishEntity::getEntityGroup,
                                TestdataLavishEntity::getValue,
                                TestdataLavishEntity::getCode,
                                TestdataLavishEntity::getLongProperty)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var value1 = solution.getFirstEntity().getValue();
        var code1 = solution.getFirstEntity().getCode();
        long property1 = solution.getFirstEntity().getLongProperty();
        var group2 = solution.getEntityGroupList().get(1);
        var value2 = solution.getEntityList().get(1).getValue();
        var code2 = solution.getEntityList().get(1).getCode();
        long property2 = solution.getEntityList().get(1).getLongProperty();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1, value1, code1, property1),
                assertMatch(group2, value2, code2, property2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group2, value2, code2, property2));
    }

    @Override
    @TestTemplate
    public void expandToBi() {
        var solution = TestdataLavishSolution.generateSolution(1, 2, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .expand(TestdataLavishEntity::getEntityGroup)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), group1),
                assertMatch(solution.getEntityList().get(1), group2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), group2));
    }

    @Override
    @TestTemplate
    public void expandToTri() {
        var solution = TestdataLavishSolution.generateSolution(1, 2, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .expand(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var value1 = solution.getFirstEntity().getValue();
        var group2 = solution.getEntityGroupList().get(1);
        var value2 = solution.getEntityList().get(1).getValue();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), group1, value1),
                assertMatch(solution.getEntityList().get(1), group2, value2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), group2, value2));
    }

    @Override
    @TestTemplate
    public void expandToQuad() {
        var solution = TestdataLavishSolution.generateSolution(1, 2, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .expand(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getValue,
                                TestdataLavishEntity::getCode)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var value1 = solution.getFirstEntity().getValue();
        var code1 = solution.getFirstEntity().getCode();
        var group2 = solution.getEntityGroupList().get(1);
        var value2 = solution.getEntityList().get(1).getValue();
        var code2 = solution.getEntityList().get(1).getCode();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), group1, value1, code1),
                assertMatch(solution.getEntityList().get(1), group2, value2, code2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), group2, value2, code2));
    }

    @Override
    @TestTemplate
    public void flattenLastWithDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .flattenLast(entity -> Arrays.asList(group1, group1, group2))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group1),
                assertMatch(group2),
                assertMatch(group1),
                assertMatch(group1),
                assertMatch(group2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group1),
                assertMatch(group2));
    }

    @Override
    @TestTemplate
    public void flattenLastWithoutDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .flattenLast(entity -> Collections.singletonList(entity.getEntityGroup()))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group2));
    }

    @Override
    @TestTemplate
    public void flattenLastAndDistinctWithDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .flattenLast(entity -> Arrays.asList(group1, group1, group2))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group2));
    }

    @Override
    @TestTemplate
    public void flattenLastAndDistinctWithoutDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .flattenLast(entity -> Collections.singletonList(entity.getEntityGroup()))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        var group1 = solution.getFirstEntityGroup();
        var group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(group1),
                assertMatch(group2));

        var entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(group2));
    }

    @Override
    @TestTemplate
    public void concatUniWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity3));
    }

    @Override
    @TestTemplate
    public void concatUniWithValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity1));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctUniWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity3));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctUniWithValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1));
    }

    @Override
    @TestTemplate
    public void concatBiWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, null),
                assertMatch(entity2, entity3));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, null),
                assertMatch(entity3, entity2));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctBiWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3)))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, null),
                assertMatch(entity2, entity3));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, null),
                assertMatch(entity3, entity2));
    }

    @Override
    @TestTemplate
    public void concatTriWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value1)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, null, null),
                assertMatch(entity2, entity3, entity1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, null, null),
                assertMatch(entity3, entity2, entity1));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctTriWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value1)))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, null, null),
                assertMatch(entity2, entity3, entity1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, null, null),
                assertMatch(entity3, entity2, entity1));
    }

    @Override
    @TestTemplate
    public void concatQuadWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value1))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value2)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, null, null, null),
                assertMatch(entity2, entity3, entity1, entity2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, null, null, null),
                assertMatch(entity3, entity2, entity1, entity3));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctQuadWithoutValueDuplicates() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value1))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value2)))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, null, null, null),
                assertMatch(entity2, entity3, entity1, entity2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, null, null, null),
                assertMatch(entity3, entity2, entity1, entity3));
    }

    @Override
    @TestTemplate
    public void concatAfterGroupBy() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .groupBy(TestdataLavishEntity::getValue, ConstraintCollectors.count())
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .groupBy(TestdataLavishEntity::getValue, ConstraintCollectors.count()))
                        .penalize(SimpleScore.ONE, (value, count) -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value1, 1),
                assertMatchWithScore(-1, value2, 1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value1, 1),
                assertMatchWithScore(-2, value2, 2));

        // Incremental for which the first change matches a join that doesn't survive the second change
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value3);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value1);
        scoreDirector.afterVariableChanged(entity3, "value");
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value1, 1),
                assertMatchWithScore(-1, value2, 1));
    }

    @Override
    @TestTemplate
    public void complement() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        var value1 = solution.getFirstValue();
        var value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        var entity1 = solution.getFirstEntity();
        var entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .complement(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity3));

        // Incremental; all entities are still present.
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value1);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity3));
    }

    @Override
    @TestTemplate
    public void penalizeUnweighted() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-7));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void penalizeUnweightedLong() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .penalizeLong(SimpleLongScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-7));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void penalizeUnweightedBigDecimal() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] { factory.forEach(TestdataEntity.class)
                                .penalizeBigDecimal(SimpleBigDecimalScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME) });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-7)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    private <Score_ extends Score<Score_>, Solution_, Entity_> void assertDefaultJustifications(
            InnerScoreDirector<Solution_, Score_> scoreDirector, List<Entity_> entityList) {
        if (!implSupport.isConstreamMatchEnabled())
            return;

        assertThat(scoreDirector.getIndictmentMap())
                .containsOnlyKeys(entityList);

        var constraintFqn =
                ConstraintRef.composeConstraintId(scoreDirector.getSolutionDescriptor()
                        .getSolutionClass().getPackageName(), TEST_CONSTRAINT_NAME);
        var constraintMatchTotalMap = scoreDirector.getConstraintMatchTotalMap();
        assertThat(constraintMatchTotalMap)
                .containsOnlyKeys(constraintFqn);
        var constraintMatchTotal = constraintMatchTotalMap.get(constraintFqn);
        assertThat(constraintMatchTotal.getConstraintMatchSet())
                .hasSize(entityList.size());
        List<ConstraintMatch<Score_>> constraintMatchList = new ArrayList<>(constraintMatchTotal.getConstraintMatchSet());
        for (var i = 0; i < entityList.size(); i++) {
            var entity = entityList.get(i);
            var constraintMatch = constraintMatchList.get(i);
            assertSoftly(softly -> {
                var justification = constraintMatch.getJustification();
                softly.assertThat(justification)
                        .isInstanceOf(DefaultConstraintJustification.class);
                var castJustification =
                        (DefaultConstraintJustification) justification;
                softly.assertThat(castJustification.getFacts())
                        .containsExactly(entity);
                softly.assertThat(constraintMatch.getIndictedObjectList())
                        .containsExactly(entity);
            });
        }
    }

    @Override
    @TestTemplate
    public void penalize() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE, entity -> 2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void penalizeLong() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .penalizeLong(SimpleLongScore.ONE, entity -> 2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void penalizeBigDecimal() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] { factory.forEach(TestdataEntity.class)
                                .penalizeBigDecimal(SimpleBigDecimalScore.ONE, entity -> BigDecimal.valueOf(2))
                                .asConstraint(TEST_CONSTRAINT_NAME) });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-14)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void rewardUnweighted() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .reward(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(7));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void reward() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .reward(SimpleScore.ONE, entity -> 2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void rewardLong() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .rewardLong(SimpleLongScore.ONE, entity -> 2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void rewardBigDecimal() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .rewardBigDecimal(SimpleBigDecimalScore.ONE, entity -> BigDecimal.valueOf(2))
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(14)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositiveUnweighted() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .impact(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(7));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositive() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .impact(SimpleScore.ONE, entity -> 2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositiveLong() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .impactLong(SimpleLongScore.ONE, entity -> 2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositiveBigDecimal() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                entity -> BigDecimal.valueOf(2))
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(14)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactNegative() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .impact(SimpleScore.ONE, entity -> -2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactNegativeLong() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .impactLong(SimpleLongScore.ONE, entity -> -2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-14));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactNegativeBigDecimal() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                entity -> BigDecimal.valueOf(-2))
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-14)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void penalizeUnweightedCustomJustifications() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-7));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    private <Score_ extends Score<Score_>, Solution_, Entity_> void assertCustomJustifications(
            InnerScoreDirector<Solution_, Score_> scoreDirector, List<Entity_> entityList) {
        if (!implSupport.isConstreamMatchEnabled())
            return;

        assertThat(scoreDirector.getIndictmentMap())
                .containsOnlyKeys(entityList);

        var constraintFqn =
                ConstraintRef.composeConstraintId(scoreDirector.getSolutionDescriptor()
                        .getSolutionClass().getPackageName(), TEST_CONSTRAINT_NAME);
        var constraintMatchTotalMap = scoreDirector.getConstraintMatchTotalMap();
        assertThat(constraintMatchTotalMap)
                .containsOnlyKeys(constraintFqn);
        var constraintMatchTotal = constraintMatchTotalMap.get(constraintFqn);
        assertThat(constraintMatchTotal.getConstraintMatchSet())
                .hasSize(entityList.size());
        List<ConstraintMatch<Score_>> constraintMatchList = new ArrayList<>(constraintMatchTotal.getConstraintMatchSet());
        for (var i = 0; i < entityList.size(); i++) {
            var entity = entityList.get(i);
            var constraintMatch = constraintMatchList.get(i);
            assertSoftly(softly -> {
                var justification = constraintMatch.getJustification();
                softly.assertThat(justification)
                        .isInstanceOf(TestConstraintJustification.class);
                var castJustification =
                        (TestConstraintJustification<Score_>) justification;
                softly.assertThat(castJustification.getFacts())
                        .containsExactly(entity);
                softly.assertThat(constraintMatch.getIndictedObjectList())
                        .containsExactly(entity);
            });
        }
    }

    @Override
    @TestTemplate
    public void penalizeCustomJustifications() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE, entity -> 2)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void penalizeLongCustomJustifications() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .penalizeLong(SimpleLongScore.ONE, entity -> 2L)
                                .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                .indictWith(Set::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void penalizeBigDecimalCustomJustifications() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] { factory.forEach(TestdataEntity.class)
                                .penalizeBigDecimal(SimpleBigDecimalScore.ONE, entity -> BigDecimal.valueOf(2))
                                .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                .indictWith(Set::of)
                                .asConstraint(TEST_CONSTRAINT_NAME) });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-14)));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void rewardUnweightedCustomJustifications() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .reward(SimpleScore.ONE)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(7));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void rewardCustomJustifications() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .reward(SimpleScore.ONE, entity -> 2)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void rewardLongCustomJustifications() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .rewardLong(SimpleLongScore.ONE, entity -> 2L)
                                .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                .indictWith(Set::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void rewardBigDecimalCustomJustifications() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .rewardBigDecimal(SimpleBigDecimalScore.ONE, entity -> BigDecimal.valueOf(2))
                                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                        .indictWith(Set::of)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(14)));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositiveUnweightedCustomJustifications() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .impact(SimpleScore.ONE)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(7));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositiveCustomJustifications() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .impact(SimpleScore.ONE, entity -> 2)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositiveLongCustomJustifications() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .impactLong(SimpleLongScore.ONE, entity -> 2L)
                                .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                .indictWith(Set::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactPositiveBigDecimalCustomJustifications() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                entity -> BigDecimal.valueOf(2))
                                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                        .indictWith(Set::of)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(14)));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactNegativeCustomJustifications() {
        var solution = TestdataLavishSolution.generateSolution();

        var scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .impact(SimpleScore.ONE, entity -> -2)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactNegativeLongCustomJustifications() {
        var solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataEntity.class)
                                .impactLong(SimpleLongScore.ONE, entity -> -2L)
                                .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                .indictWith(Set::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-14));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void impactNegativeBigDecimalCustomJustifications() {
        var solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                entity -> BigDecimal.valueOf(-2))
                                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                                        .indictWith(Set::of)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-14)));
        assertCustomJustifications(scoreDirector, solution.getEntityList());
    }

    @Override
    @TestTemplate
    public void failWithMultipleJustifications() {
        assertThatCode(() -> buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE, entity -> 2)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME)))
                .hasMessageContaining("Maybe the constraint calls justifyWith() twice?");
    }

    @Override
    @TestTemplate
    public void failWithMultipleIndictments() {
        assertThatCode(() -> buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE, entity -> 2)
                        .justifyWith((a, score) -> new TestConstraintJustification<>(score, a))
                        .indictWith(Set::of)
                        .indictWith(Set::of)
                        .asConstraint(TEST_CONSTRAINT_NAME)))
                .hasMessageContaining("Maybe the constraint calls indictWith() twice?");
    }

    // ************************************************************************
    // Combinations
    // ************************************************************************

    @TestTemplate
    public void duplicateConstraintId() {
        ConstraintProvider constraintProvider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("duplicateConstraintName"),
                factory.forEach(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("duplicateConstraintName")
        };
        assertThatIllegalStateException().isThrownBy(() -> buildScoreDirector(
                TestdataLavishSolution.buildSolutionDescriptor(),
                constraintProvider));
    }

    @TestTemplate
    public void zeroConstraintWeightDisabled() {
        var solution = TestdataLavishSolution.generateSolution(2, 5, 3, 2);
        var entity1 = new TestdataLavishEntity("MyEntity 1", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        entity1.setStringProperty("myProperty1");
        solution.getEntityList().add(entity1);

        var zeroWeightMonitorCount = new AtomicLong(0L);
        var oneWeightMonitorCount = new AtomicLong(0L);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataLavishSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> {
                                    zeroWeightMonitorCount.getAndIncrement();
                                    return true;
                                })
                                .penalize(SimpleScore.ZERO)
                                .asConstraint("myConstraint1"),
                        factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> {
                                    oneWeightMonitorCount.getAndIncrement();
                                    return true;
                                })
                                .penalize(SimpleScore.ONE)
                                .asConstraint("myConstraint2")
                });

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(zeroWeightMonitorCount.getAndSet(0L)).isEqualTo(0);
        assertThat(oneWeightMonitorCount.getAndSet(0L)).isEqualTo(3);

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity1);
        entity1.setStringProperty("myProperty2");
        scoreDirector.afterProblemPropertyChanged(entity1);
        scoreDirector.calculateScore();
        assertThat(zeroWeightMonitorCount.get()).isEqualTo(0);
        assertThat(oneWeightMonitorCount.get()).isEqualTo(1);
    }

    @TestTemplate
    @Deprecated(forRemoval = true)
    public void fromIncludesNullWhenAllowsUnassigned() {
        var solution = TestdataAllowsUnassignedSolution.generateSolution();
        var entityWithNull = solution.getEntityList().get(0);
        var entityWithValue = solution.getEntityList().get(1);

        InnerScoreDirector<TestdataAllowsUnassignedSolution, SimpleScore> scoreDirector = buildScoreDirector(
                TestdataAllowsUnassignedSolution.buildSolutionDescriptor(),
                constraintFactory -> new Constraint[] {
                        constraintFactory.from(TestdataAllowsUnassignedEntity.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entityWithNull),
                assertMatch(entityWithValue));
    }

    @TestTemplate
    public void constraintProvidedFromUnknownPackage() throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        var clz = Class.forName("TestdataInUnnamedPackageSolution");
        var solution = clz.getMethod("generateSolution").invoke(null);
        var solutionDescriptor = (SolutionDescriptor) clz.getMethod("buildSolutionDescriptor").invoke(null);
        var entityList = (List<TestdataEntity>) clz.getMethod("getEntityList")
                .invoke(solution);
        entityList.removeIf(entity -> !Objects.equals(entity.getCode(), "Generated Entity 0"));

        InnerScoreDirector scoreDirector = buildScoreDirector(solutionDescriptor, new TestdataConstraintProvider());

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch("unnamed.package", "Always penalize", entityList.get(0)));
    }

}
