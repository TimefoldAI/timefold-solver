package ai.timefold.solver.core.impl.score.stream.common.quad;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countDistinct;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countQuad;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.max;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.min;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.toSet;
import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamFunctionalTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataSimpleBigDecimalScoreSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataSimpleLongScoreSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishExtra;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishValue;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishValueGroup;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;

public abstract class AbstractQuadConstraintStreamTest
        extends AbstractConstraintStreamTest
        implements ConstraintStreamFunctionalTest {

    protected AbstractQuadConstraintStreamTest(ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @Override
    @TestTemplate
    public void filter_entity() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 0, 1, 0);
        TestdataLavishValue value1 = new TestdataLavishValue("MyValue 1", solution.getFirstValueGroup());
        solution.getValueList().add(value1);
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        solution.getValueList().add(value2);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(), value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity3);
        TestdataLavishExtra extra1 = new TestdataLavishExtra("MyExtra 1");
        solution.getExtraList().add(extra1);
        TestdataLavishExtra extra2 = new TestdataLavishExtra("MyExtra 2");
        solution.getExtraList().add(extra2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class, equal((e1, e2) -> e1.getValue(), identity()))
                        .join(TestdataLavishExtra.class)
                        .filter((e1, e2, value, extra) -> value.getCode().equals("MyValue 1")
                                && extra.getCode().equals("MyExtra 1"))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, value1, extra1),
                assertMatch(entity1, entity3, value1, extra1));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setValue(value2);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, value1, extra1),
                assertMatch(entity1, entity3, value1, extra1));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(entity2);
        entity2.setValue(value1);
        scoreDirector.afterProblemPropertyChanged(entity2);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, value1, extra1),
                assertMatch(entity1, entity3, value1, extra1),
                assertMatch(entity2, entity3, value1, extra1));
    }

    @Override
    @TestTemplate
    public void filter_consecutive() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(5, 5);
        TestdataLavishEntity entity1 = solution.getEntityList().get(0);
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        TestdataLavishEntity entity4 = solution.getEntityList().get(3);
        TestdataLavishEntity entity5 = solution.getEntityList().get(4);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, equal((a, b) -> a, identity()))
                        .join(TestdataLavishEntity.class,
                                equal((a, b, c) -> a, identity()),
                                filtering((entityA, entityB, entityC, entityD) -> !Objects.equals(entityA, entity1)))
                        .filter((entityA, entityB, entityC, entityD) -> !Objects.equals(entityA, entity2))
                        .filter((entityA, entityB, entityC, entityD) -> !Objects.equals(entityA, entity3))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector, assertMatch(entity4, entity5, entity4, entity4));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity4);
        solution.getEntityList().remove(entity4);
        scoreDirector.afterEntityRemoved(entity4);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void ifExists_unknownClass() {
        assertThatThrownBy(() -> buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                        TestdataLavishEntity::getEntityGroup))
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
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 1, 1);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .ifExists(TestdataLavishValueGroup.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        TestdataLavishValueGroup valueGroup = solution.getFirstValueGroup();
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue(),
                        solution.getFirstEntity()));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(valueGroup);
        solution.getValueGroupList().remove(valueGroup);
        scoreDirector.afterProblemFactRemoved(valueGroup);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void ifExists_0Join1Filter() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 2, 1, 1);
        TestdataLavishEntityGroup entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getValueList().get(1));
        solution.getEntityList().add(entity2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal((entityA, entityB) -> entityA.getEntityGroup(), identity()))
                .join(TestdataLavishValue.class, equal((entityA, entityB, group) -> entityA.getValue(), identity()))
                .ifExists(TestdataLavishValueGroup.class,
                        filtering((entityA, entityB, entityAGroup, value, valueGroup) -> Objects
                                .equals(value.getValueGroup(), valueGroup)))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity1, entityGroup, solution.getFirstValue()),
                assertMatch(entity2, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()),
                assertMatch(entity1, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()));

        // Incremental
        TestdataLavishValueGroup toRemove = solution.getFirstValueGroup();
        scoreDirector.beforeProblemFactRemoved(toRemove);
        solution.getValueGroupList().remove(toRemove);
        scoreDirector.afterProblemFactRemoved(toRemove);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void ifExists_1Join0Filter() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishEntityGroup entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal((entityA, entityB) -> entityA.getEntityGroup(), identity()))
                .join(TestdataLavishValue.class, equal((entityA, entityB, group) -> entityA.getValue(), identity()))
                .ifExists(TestdataLavishEntityGroup.class,
                        equal((entityA, entityB, groupA, valueA) -> entityA.getEntityGroup(), identity()))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity1, entityGroup, solution.getFirstValue()),
                assertMatch(entity2, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()),
                assertMatch(entity1, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector,
                assertMatch(entity2, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()),
                assertMatch(entity1, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()));
    }

    @Override
    @TestTemplate
    public void ifExists_1Join1Filter() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishEntityGroup entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal((entityA, entityB) -> entityA.getEntityGroup(), identity()))
                .join(TestdataLavishValue.class, equal((entityA, entityB, group) -> entityA.getValue(), identity()))
                .ifExists(TestdataLavishEntityGroup.class,
                        equal((entityA, entityB, groupA, valueA) -> entityA.getEntityGroup(), identity()),
                        filtering((entityA, entityB, groupA, valueA, groupB) -> entityA.getCode().contains("MyEntity")
                                || groupA.getCode().contains("MyEntity")))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity1, entityGroup, solution.getFirstValue()));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    @Disabled("Would cause too many matches to meaningfully assert; cost-benefit ratio is wrong here.")
    public void ifExistsDoesNotIncludeUnassigned() {

    }

    @Override
    @TestTemplate
    @Deprecated(forRemoval = true)
    @Disabled("Would cause too many matches to meaningfully assert; cost-benefit ratio is wrong here.")
    public void ifExistsIncludesNullVarsWithFrom() {
    }

    @Override
    @TestTemplate
    public void ifNotExists_unknownClass() {
        assertThatThrownBy(() -> buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                        TestdataLavishEntity::getEntityGroup))
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
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 1, 1);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .ifNotExists(TestdataLavishValueGroup.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        TestdataLavishValueGroup valueGroup = solution.getFirstValueGroup();
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        // Incremental
        scoreDirector.beforeProblemFactRemoved(valueGroup);
        solution.getValueGroupList().remove(valueGroup);
        scoreDirector.afterProblemFactRemoved(valueGroup);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue(),
                        solution.getFirstEntity()));
    }

    @Override
    @TestTemplate
    public void ifNotExists_0Join1Filter() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 2, 1, 1);
        TestdataLavishEntityGroup entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getValueList().get(1));
        solution.getEntityList().add(entity2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal((entityA, entityB) -> entityA.getEntityGroup(), identity()))
                .join(TestdataLavishValue.class, equal((entityA, entityB, group) -> entityA.getValue(), identity()))
                .ifNotExists(TestdataLavishValueGroup.class,
                        filtering((entityA, entityB, entityAGroup, value, valueGroup) -> Objects
                                .equals(value.getValueGroup(), valueGroup)))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        // Incremental
        TestdataLavishValueGroup toRemove = solution.getFirstValueGroup();
        scoreDirector.beforeProblemFactRemoved(toRemove);
        solution.getValueGroupList().remove(toRemove);
        scoreDirector.afterProblemFactRemoved(toRemove);
        assertScore(scoreDirector,
                assertMatch(entity2, entity1, entityGroup, solution.getFirstValue()),
                assertMatch(entity2, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()),
                assertMatch(entity1, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()));
    }

    @Override
    @TestTemplate
    public void ifNotExists_1Join0Filter() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishEntityGroup entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal((entityA, entityB) -> entityA.getEntityGroup(), identity()))
                .join(TestdataLavishValue.class, equal((entityA, entityB, group) -> entityA.getValue(), identity()))
                .ifNotExists(TestdataLavishEntityGroup.class,
                        equal((entityA, entityB, groupA, valueA) -> entityB.getEntityGroup(), identity()))
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
                assertMatch(entity1, solution.getFirstEntity(), solution.getFirstEntityGroup(), solution.getFirstValue()));
    }

    @Override
    @TestTemplate
    public void ifNotExists_1Join1Filter() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishEntityGroup entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class)
                .join(TestdataLavishEntityGroup.class, equal((entityA, entityB) -> entityA.getEntityGroup(), identity()))
                .join(TestdataLavishValue.class, equal((entityA, entityB, group) -> entityA.getValue(), identity()))
                .ifNotExists(TestdataLavishEntityGroup.class,
                        equal((entityA, entityB, groupA, valueA) -> entityA.getEntityGroup(), identity()),
                        filtering((entityA, entityB, groupA, valueA,
                                groupB) -> !(entityA.getCode().contains("MyEntity")
                                        && groupB.getCode().contains("MyEntity"))))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity1, entityGroup, solution.getFirstValue()));

        // Incremental
        scoreDirector.beforeProblemFactRemoved(entityGroup);
        solution.getEntityGroupList().remove(entityGroup);
        scoreDirector.afterProblemFactRemoved(entityGroup);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    @Disabled("Would cause too many matches to meaningfully assert; cost-benefit ratio is wrong here.")
    public void ifNotExistsDoesNotIncludeUnassigned() {

    }

    @Override
    @TestTemplate
    @Deprecated(forRemoval = true)
    @Disabled("Would cause too many matches to meaningfully assert; cost-benefit ratio is wrong here.")
    public void ifNotExistsIncludesNullVarsWithFrom() {

    }

    @Override
    @TestTemplate
    public void ifExistsAfterGroupBy() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 0, 1, 0);
        TestdataLavishValue value1 = new TestdataLavishValue("MyValue 1", solution.getFirstValueGroup());
        solution.getValueList().add(value1);
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        solution.getValueList().add(value2);
        TestdataLavishEntity entity1 = new TestdataLavishEntity("MyEntity 1", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity1);
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(), value1);
        solution.getEntityList().add(entity2);
        TestdataLavishExtra extra1 = new TestdataLavishExtra("MyExtra 1");
        solution.getExtraList().add(extra1);
        TestdataLavishExtra extra2 = new TestdataLavishExtra("MyExtra 2");
        solution.getExtraList().add(extra2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(countDistinct(TestdataLavishEntity::getValue),
                                countDistinct(TestdataLavishEntity::getValue),
                                countDistinct(TestdataLavishEntity::getValue),
                                countDistinct(TestdataLavishEntity::getValue))
                        .ifExists(TestdataLavishExtra.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(1, 1, 1, 1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value2);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(2, 2, 2, 2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity2);
        solution.getEntityList().remove(entity2);
        scoreDirector.afterEntityRemoved(entity2);
        assertScore(scoreDirector,
                assertMatch(1, 1, 1, 1));
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping1Collector() {
        /*
         * E1 has G1 and V1
         * E2 has G2 and V2
         * E3 has G1 and V1
         */
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy(countQuad())
                        .penalize(SimpleScore.ONE, count -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-5, 5)); // E1 G1 V1 E1, E1 G1 V1 E3, E2 G2 V2 E2, E3 G1 V1 E1, E3 G1 V1 E3

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-2, 2)); // E2 G2 V2 E2, E3 G1 V1 E3
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping2Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, equal((e1, e2) -> e1, Function.identity()))
                        .join(TestdataLavishEntity.class, equal((e1, e2, e3) -> e2, Function.identity()))
                        .groupBy(countQuad(),
                                countDistinct((e, e2, e3, e4) -> e))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity1 = solution.getFirstEntity();

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector, assertMatchWithScore(-1, 3, 2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector, assertMatchWithScore(-1, 1, 1));
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping3Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, equal((e1, e2) -> e1, Function.identity()))
                        .join(TestdataLavishEntity.class, equal((e1, e2, e3) -> e2, Function.identity()))
                        .groupBy(countQuad(),
                                min((TestdataLavishEntity e, TestdataLavishEntity e2, TestdataLavishEntity e3,
                                        TestdataLavishEntity e4) -> e.getLongProperty()),
                                max((TestdataLavishEntity e, TestdataLavishEntity e2, TestdataLavishEntity e3,
                                        TestdataLavishEntity e4) -> e.getLongProperty()))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity1 = solution.getFirstEntity();
        entity1.setLongProperty(0L);
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        entity2.setLongProperty(1L);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        entity3.setLongProperty(2L);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 3, 0L, 1L));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 1, 1L, 1L));
    }

    @Override
    @TestTemplate
    public void groupBy_0Mapping4Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, equal((e1, e2) -> e1, Function.identity()))
                        .join(TestdataLavishEntity.class, equal((e1, e2, e3) -> e2, Function.identity()))
                        .groupBy(countQuad(),
                                min((TestdataLavishEntity e, TestdataLavishEntity e2, TestdataLavishEntity e3,
                                        TestdataLavishEntity e4) -> e.getLongProperty()),
                                max((TestdataLavishEntity e, TestdataLavishEntity e2, TestdataLavishEntity e3,
                                        TestdataLavishEntity e4) -> e.getLongProperty()),
                                toSet((e, e2, e3, e4) -> e))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity1 = solution.getFirstEntity();
        entity1.setLongProperty(0L);
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        entity2.setLongProperty(1L);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        entity3.setLongProperty(2L);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 3, 0L, 1L, asSet(entity1, entity2)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, 1, 1L, 1L, asSet(entity2)));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping0Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> value)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = solution.getValueList().get(1);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value2),
                assertMatchWithScore(-1, value1));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping1Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> value, countQuad())
                        .penalize(SimpleScore.ONE, (group, count) -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = solution.getValueList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value2, 1),
                assertMatchWithScore(-4, value1, 4));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value2, 1),
                assertMatchWithScore(-1, value1, 1));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping2Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, equal((entityA, entityB) -> entityA, Function.identity()))
                        .join(TestdataLavishEntity.class, equal((entityA, entityB, entityC) -> entityB, Function.identity()))
                        .groupBy((entityA, entityB, entityC, entityD) -> entityA.toString(),
                                countQuad(),
                                toSet((entityA, entityB, entityC, entityD) -> entityA))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, null, TEST_CONSTRAINT_NAME, entity1.toString(), 2, singleton(entity1)),
                assertMatchWithScore(-1, null, TEST_CONSTRAINT_NAME, entity2.toString(), 1, singleton(entity2)));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, null, TEST_CONSTRAINT_NAME, entity2.toString(), 1, singleton(entity2)));
    }

    @Override
    @TestTemplate
    public void groupBy_1Mapping3Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(factory -> factory
                .forEachUniquePair(TestdataLavishEntity.class)
                .join(TestdataLavishEntity.class, equal((entityA, entityB) -> entityA, Function.identity()))
                .join(TestdataLavishEntity.class, equal((entityA, entityB, entityC) -> entityB, Function.identity()))
                .groupBy((entityA, entityB, entityC, entityD) -> entityA.toString(),
                        min((TestdataLavishEntity entityA, TestdataLavishEntity entityB,
                                TestdataLavishEntity entityC, TestdataLavishEntity entityD) -> entityA.getLongProperty()),
                        max((TestdataLavishEntity entityA, TestdataLavishEntity entityB,
                                TestdataLavishEntity entityC, TestdataLavishEntity entityD) -> entityA.getLongProperty()),
                        toSet((entityA, entityB, entityC, entityD) -> entityA))
                .penalize(SimpleScore.ONE)
                .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity1 = solution.getFirstEntity();
        entity1.setLongProperty(Long.MAX_VALUE);
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        entity2.setLongProperty(Long.MIN_VALUE);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, null, TEST_CONSTRAINT_NAME, entity1.toString(), Long.MAX_VALUE, Long.MAX_VALUE,
                        singleton(entity1)),
                assertMatchWithScore(-1, null, TEST_CONSTRAINT_NAME, entity2.toString(), Long.MIN_VALUE, Long.MIN_VALUE,
                        singleton(entity2)));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, null, TEST_CONSTRAINT_NAME, entity2.toString(), Long.MIN_VALUE, Long.MIN_VALUE,
                        singleton(entity2)));
    }

    @Override
    @TestTemplate
    public void groupBy_2Mapping0Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> group, (entity1, group, value, entity2) -> value)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = solution.getValueList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, value2),
                assertMatchWithScore(-1, group1, value1));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, value2),
                assertMatchWithScore(-1, group1, value1));
    }

    @Override
    @TestTemplate
    public void groupBy_2Mapping1Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> group, (entity1, group, value, entity2) -> value,
                                countQuad())
                        .penalize(SimpleScore.ONE, (group, value, count) -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = solution.getValueList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, value2, 1),
                assertMatchWithScore(-4, group1, value1, 4));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, value2, 1),
                assertMatchWithScore(-1, group1, value1, 1));
    }

    @Override
    @TestTemplate
    public void groupBy_2Mapping2Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> group, (entity1, group, value, entity2) -> value,
                                countQuad(), countQuad())
                        .penalize(SimpleScore.ONE, (group, value, count, sameCount) -> count + sameCount)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = solution.getValueList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-2, group2, value2, 1, 1),
                assertMatchWithScore(-8, group1, value1, 4, 4));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-2, group2, value2, 1, 1),
                assertMatchWithScore(-2, group1, value1, 1, 1));
    }

    @Override
    @TestTemplate
    public void groupBy_3Mapping0Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> entity1.getEntityGroup(),
                                (entity1, group, value, entity2) -> entity2.getEntityGroup(),
                                (entity1, group, value, entity2) -> value)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = solution.getValueList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, group2, value2),
                assertMatchWithScore(-1, group1, group1, value1));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, value2, value2),
                assertMatchWithScore(-1, group1, value1, value1));
    }

    @Override
    @TestTemplate
    public void groupBy_3Mapping1Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> entity1.getEntityGroup(),
                                (entity1, group, value, entity2) -> entity2.getEntityGroup(),
                                (entity1, group, value, entity2) -> group, ConstraintCollectors.countQuad())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, group2, group2, 1),
                assertMatchWithScore(-1, group1, group1, group1, 4));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, group2, group2, 1),
                assertMatchWithScore(-1, group1, group1, group1, 1));
    }

    @Override
    @TestTemplate
    public void groupBy_4Mapping0Collector() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 2, 2, 3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class, equal(TestdataLavishEntity::getEntityGroup, identity()))
                        .join(TestdataLavishValue.class, equal((entity, group) -> entity.getValue(), identity()))
                        .join(TestdataLavishEntity.class, equal((entity, group, value) -> group,
                                TestdataLavishEntity::getEntityGroup))
                        .groupBy((entity1, group, value, entity2) -> entity1.getEntityGroup(),
                                (entity1, group, value, entity2) -> entity2.getEntityGroup(),
                                (entity1, group, value, entity2) -> group,
                                (entity1, group, value, entity2) -> value)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = solution.getValueList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, group2, group2, value2),
                assertMatchWithScore(-1, group1, group1, group1, value1));

        // Incremental
        TestdataLavishEntity entity = solution.getFirstEntity();
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, group2, group2, group2, value2),
                assertMatchWithScore(-1, group1, group1, group1, value1));
    }

    @Override
    @TestTemplate
    public void distinct() { // On a distinct stream, this is a no-op.
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 2, 2, 3);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, group1, group2),
                assertMatch(entity1, entity3, group1, group1),
                assertMatch(entity2, entity3, group2, group1));
    }

    @Override
    @TestTemplate
    public void mapToUniWithDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 2, 2, 3);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .map((entity1, entity2, group1, group2) -> asSet(group1, group2))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity = solution.getFirstEntity();
        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(asSet(group1, group2)),
                assertMatch(asSet(group1, group2)),
                assertMatch(asSet(group1)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(asSet(group1, group2)));
    }

    @Override
    @TestTemplate
    public void mapToUniWithoutDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .map((entity1, entity2, group1, group2) -> asSet(group1, group2))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(asSet(group1, group2)));

        TestdataLavishEntity entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void mapToUniAndDistinctWithDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 2, 2, 3);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .map((entity1, entity2, group1, group2) -> asSet(group1, group2))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntity entity = solution.getFirstEntity();
        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(asSet(group1, group2)),
                assertMatch(asSet(group1)));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector,
                assertMatch(asSet(group1, group2)));
    }

    @Override
    @TestTemplate
    public void mapToUniAndDistinctWithoutDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .map((entity1, entity2, group1, group2) -> asSet(group1, group2))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(asSet(group1, group2)));

        TestdataLavishEntity entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void mapToBi() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .map((entity1, entity2, group1, group2) -> entity1,
                                (entity1, entity2, group1, group2) -> entity2)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), solution.getEntityList().get(1)));

        TestdataLavishEntity entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void mapToTri() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .map((entity1, entity2, group1, group2) -> entity1,
                                (entity1, entity2, group1, group2) -> entity2,
                                (entity1, entity2, group1, group2) -> entity1.getLongProperty() + entity2.getLongProperty())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(solution.getFirstEntity(), solution.getEntityList().get(1),
                        solution.getFirstEntity().getLongProperty() + solution.getEntityList().get(1).getLongProperty()));

        TestdataLavishEntity entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void mapToQuad() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 2);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2) -> entity1.getEntityGroup(), Function.identity()))
                        .join(TestdataLavishEntityGroup.class,
                                Joiners.equal((entity1, entity2, group) -> entity2.getEntityGroup(), Function.identity()))
                        .map((entity1, entity2, group1, group2) -> entity1.getCode(),
                                (entity1, entity2, group1, group2) -> entity2.getCode(),
                                (entity1, entity2, group1, group2) -> group1.getCode(),
                                (entity1, entity2, group1, group2) -> group2.getCode())
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(null, TEST_CONSTRAINT_NAME, solution.getFirstEntity().getCode(),
                        solution.getEntityList().get(1).getCode(), solution.getFirstEntityGroup().getCode(),
                        solution.getEntityGroupList().get(1).getCode()));

        TestdataLavishEntity entity = solution.getFirstEntity();

        // Incremental
        scoreDirector.beforeEntityRemoved(entity);
        solution.getEntityList().remove(entity);
        scoreDirector.afterEntityRemoved(entity);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void flattenLastWithDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, filtering((a, b, c) -> a != c && b != c))
                        .join(TestdataLavishEntity.class, filtering((a, b, c, d) -> a != d && b != d))
                        .flattenLast((d) -> asList(group1, group1, group2))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, group1),
                assertMatch(entity1, entity2, entity3, group1),
                assertMatch(entity1, entity2, entity3, group2),
                assertMatch(entity1, entity3, entity2, group1),
                assertMatch(entity1, entity3, entity2, group1),
                assertMatch(entity1, entity3, entity2, group2),
                assertMatch(entity2, entity3, entity1, group1),
                assertMatch(entity2, entity3, entity1, group1),
                assertMatch(entity2, entity3, entity1, group2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void flattenLastWithoutDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, filtering((a, b, c) -> a != c && b != c))
                        .join(TestdataLavishEntity.class, filtering((a, b, c, d) -> a != d && b != d))
                        .flattenLast((d) -> asList(group1, group2))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, group1),
                assertMatch(entity1, entity2, entity3, group2),
                assertMatch(entity1, entity3, entity2, group1),
                assertMatch(entity1, entity3, entity2, group2),
                assertMatch(entity2, entity3, entity1, group1),
                assertMatch(entity2, entity3, entity1, group2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void flattenLastAndDistinctWithDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, filtering((a, b, c) -> a != c && b != c))
                        .join(TestdataLavishEntity.class, filtering((a, b, c, d) -> a != d && b != d))
                        .flattenLast((d) -> asList(group1, group1, group2))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, group1),
                assertMatch(entity1, entity2, entity3, group2),
                assertMatch(entity1, entity3, entity2, group1),
                assertMatch(entity1, entity3, entity2, group2),
                assertMatch(entity2, entity3, entity1, group1),
                assertMatch(entity2, entity3, entity1, group2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void flattenLastAndDistinctWithoutDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(1, 1, 2, 3);
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = solution.getEntityList().get(1);
        TestdataLavishEntity entity3 = solution.getEntityList().get(2);
        TestdataLavishEntityGroup group1 = solution.getFirstEntityGroup();
        TestdataLavishEntityGroup group2 = solution.getEntityGroupList().get(1);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEachUniquePair(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class, filtering((a, b, c) -> a != c && b != c))
                        .join(TestdataLavishEntity.class, filtering((a, b, c, d) -> a != d && b != d))
                        .flattenLast((d) -> asList(group1, group2))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, group1),
                assertMatch(entity1, entity2, entity3, group2),
                assertMatch(entity1, entity3, entity2, group1),
                assertMatch(entity1, entity3, entity2, group2),
                assertMatch(entity2, entity3, entity1, group1),
                assertMatch(entity2, entity3, entity1, group2));

        // Incremental
        scoreDirector.beforeEntityRemoved(entity1);
        solution.getEntityList().remove(entity1);
        scoreDirector.afterEntityRemoved(entity1);
        assertScore(scoreDirector);
    }

    @Override
    @TestTemplate
    public void concatUniWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, null, null, null));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, null, null, null));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctUniWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .distinct()
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, null, null, null));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, null, null, null));
    }

    @Override
    @TestTemplate
    public void concatBiWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, entity3, null, null));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, entity2, null, null));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctBiWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
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
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, entity3, null, null));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, entity2, null, null));
    }

    @Override
    @TestTemplate
    public void concatTriWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
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
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, entity3, entity1, null));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, entity2, entity1, null));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctTriWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
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
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, entity3, entity1, null));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, entity2, entity1, null));
    }

    @Override
    @TestTemplate
    public void concatQuadWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
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
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, entity3, entity1, entity2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, entity2, entity1, entity3));
    }

    @Override
    @TestTemplate
    public void concatQuadWithValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value2))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value3))
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value1)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity1, entity2, entity3, entity1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity1, entity3, entity2, entity1));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctQuadWithoutValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
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
                assertMatch(entity1, entity2, entity3, entity1),
                assertMatch(entity2, entity3, entity1, entity2));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1),
                assertMatch(entity3, entity2, entity1, entity3));
    }

    @Override
    @TestTemplate
    public void concatAndDistinctQuadWithValueDuplicates() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .filter(entity -> entity.getValue() == value1)
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value2))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value3))
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1))
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .filter(entity -> entity.getValue() == value1)
                                .join(factory.forEach(TestdataLavishEntity.class)
                                        .filter(entity -> entity.getValue() == value2))
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
                assertMatch(entity1, entity2, entity3, entity1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value3);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity3, entity2, entity1));
    }

    @Override
    @TestTemplate
    public void concatAfterGroupBy() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution(2, 5, 1, 1);
        TestdataLavishValue value1 = solution.getFirstValue();
        TestdataLavishValue value2 = new TestdataLavishValue("MyValue 2", solution.getFirstValueGroup());
        TestdataLavishValue value3 = new TestdataLavishValue("MyValue 3", solution.getFirstValueGroup());
        TestdataLavishEntity entity1 = solution.getFirstEntity();
        TestdataLavishEntity entity2 = new TestdataLavishEntity("MyEntity 2", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity2);
        TestdataLavishEntity entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value3);
        solution.getEntityList().add(entity3);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class)
                        .join(TestdataLavishEntity.class)
                        .filter((e1, e2, e3, e4) -> e1.getValue() == value1 && e2.getValue() == value2
                                && e3.getValue() == value3 && e4.getValue() == value1)
                        .groupBy((e1, e2, e3, e4) -> e1.getValue(),
                                (e1, e2, e3, e4) -> e2.getValue(),
                                (e1, e2, e3, e4) -> e3.getValue().getCode() + e4.getValue().getCode(),
                                ConstraintCollectors.countQuad())
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .join(TestdataLavishEntity.class)
                                .join(TestdataLavishEntity.class)
                                .join(TestdataLavishEntity.class)
                                .filter((e1, e2, e3, e4) -> e1.getValue() == value3 && e2.getValue() == value2
                                        && e3.getValue() == value1 && e4.getValue() == value3)
                                .groupBy((e1, e2, e3, e4) -> e1.getValue(),
                                        (e1, e2, e3, e4) -> e2.getValue(),
                                        (e1, e2, e3, e4) -> e3.getValue().getCode() + e4.getValue().getCode(),
                                        ConstraintCollectors.countQuad()))
                        .penalize(SimpleScore.ONE, (v1, v2, v3, count) -> count)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value1, value2, value3.getCode() + value1.getCode(), 1),
                assertMatchWithScore(-1, value3, value2, value1.getCode() + value3.getCode(), 1));

        // Incremental
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value2);
        scoreDirector.afterVariableChanged(entity3, "value");
        assertScore(scoreDirector);

        // Incremental for which the first change matches a join that doesn't survive the second change
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value3);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity3, "value");
        entity3.setValue(value1);
        scoreDirector.afterVariableChanged(entity3, "value");
        assertScore(scoreDirector,
                assertMatchWithScore(-1, value1, value2, value3.getCode() + value1.getCode(), 1),
                assertMatchWithScore(-1, value3, value2, value1.getCode() + value3.getCode(), 1));
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
                        .expand(a -> {
                            var code = a.getValue().getCode();
                            var indexString = code.substring(code.length() - 1);
                            return Integer.parseInt(indexString);
                        })
                        .expand((a, b) -> b + 1)
                        .expand((a, b, c) -> c + 1)
                        .filter((entity, index, index2, index3) -> index == 0)
                        .complement(TestdataLavishEntity.class, e -> Integer.MAX_VALUE, e -> -1, e -> 0)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, 0, 1, 2),
                assertMatch(entity2, Integer.MAX_VALUE, -1, 0),
                assertMatch(entity3, Integer.MAX_VALUE, -1, 0));

        // Incremental; all entities are still present, but the indexes are different.
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value1);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, 0, 1, 2),
                assertMatch(entity2, 0, 1, 2),
                assertMatch(entity3, Integer.MAX_VALUE, -1, 0));
    }

    @Override
    @TestTemplate
    public void penalizeUnweighted() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-2));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void penalizeUnweightedLong() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .penalizeLong(SimpleLongScore.ONE)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-2));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void penalizeUnweightedBigDecimal() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .penalizeBigDecimal(SimpleBigDecimalScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-2)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    private <Score_ extends Score<Score_>, Solution_, Entity_, Value_> void assertDefaultJustifications(
            InnerScoreDirector<Solution_, Score_> scoreDirector, List<Entity_> entityList, List<Value_> valueList) {
        if (!implSupport.isConstreamMatchEnabled())
            return;

        assertThat(scoreDirector.getIndictmentMap())
                .containsOnlyKeys(entityList.get(0),
                        entityList.get(1),
                        entityList.get(5),
                        entityList.get(6),
                        valueList.get(0),
                        valueList.get(1));

        String constraintFqn =
                ConstraintRef.composeConstraintId(scoreDirector.getSolutionDescriptor()
                        .getSolutionClass().getPackageName(), TEST_CONSTRAINT_NAME);
        Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap = scoreDirector.getConstraintMatchTotalMap();
        assertThat(constraintMatchTotalMap)
                .containsOnlyKeys(constraintFqn);
        ConstraintMatchTotal<Score_> constraintMatchTotal = constraintMatchTotalMap.get(constraintFqn);
        assertThat(constraintMatchTotal.getConstraintMatchSet())
                .hasSize(2);
        List<ConstraintMatch<Score_>> constraintMatchList = new ArrayList<>(constraintMatchTotal.getConstraintMatchSet());
        for (int i = 0; i < 2; i++) {
            ConstraintMatch<Score_> constraintMatch = constraintMatchList.get(i);
            assertSoftly(softly -> {
                ConstraintJustification justification = constraintMatch.getJustification();
                softly.assertThat(justification)
                        .isInstanceOf(DefaultConstraintJustification.class);
                DefaultConstraintJustification castJustification =
                        (DefaultConstraintJustification) justification;
                softly.assertThat(castJustification.getFacts())
                        .hasSize(4);
                softly.assertThat(constraintMatch.getIndictedObjectList())
                        .hasSize(4);
            });
        }
    }

    @Override
    @TestTemplate
    public void penalize() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .penalize(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void penalizeLong() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .penalizeLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> 2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void penalizeBigDecimal() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .penalizeBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(2))
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-4)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void rewardUnweighted() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .reward(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(2));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void reward() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .reward(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void rewardLong() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .rewardLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> 2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void rewardBigDecimal() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .rewardBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(2))
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(4)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositiveUnweighted() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .impact(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(2));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositive() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .impact(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositiveLong() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .impactLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> 2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositiveBigDecimal() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(2))
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(4)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactNegative() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .impact(SimpleScore.ONE, (entity, entity2, value, value2) -> -2)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactNegativeLong() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .impactLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> -2L)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-4));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactNegativeBigDecimal() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(-2))
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-4)));
        assertDefaultJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void penalizeUnweightedCustomJustifications() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .penalize(SimpleScore.ONE)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-2));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    private <Score_ extends Score<Score_>, Solution_, Entity_, Value_> void assertCustomJustifications(
            InnerScoreDirector<Solution_, Score_> scoreDirector, List<Entity_> entityList, List<Value_> valueList) {
        if (!implSupport.isConstreamMatchEnabled())
            return;

        assertThat(scoreDirector.getIndictmentMap())
                .containsOnlyKeys(entityList.get(0),
                        entityList.get(1),
                        entityList.get(5),
                        entityList.get(6),
                        valueList.get(0),
                        valueList.get(1));

        String constraintFqn =
                ConstraintRef.composeConstraintId(scoreDirector.getSolutionDescriptor()
                        .getSolutionClass().getPackageName(), TEST_CONSTRAINT_NAME);
        Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap = scoreDirector.getConstraintMatchTotalMap();
        assertThat(constraintMatchTotalMap)
                .containsOnlyKeys(constraintFqn);
        ConstraintMatchTotal<Score_> constraintMatchTotal = constraintMatchTotalMap.get(constraintFqn);
        assertThat(constraintMatchTotal.getConstraintMatchSet())
                .hasSize(2);
        List<ConstraintMatch<Score_>> constraintMatchList = new ArrayList<>(constraintMatchTotal.getConstraintMatchSet());
        for (int i = 0; i < 2; i++) {
            ConstraintMatch<Score_> constraintMatch = constraintMatchList.get(i);
            assertSoftly(softly -> {
                ConstraintJustification justification = constraintMatch.getJustification();
                softly.assertThat(justification)
                        .isInstanceOf(TestConstraintJustification.class);
                TestConstraintJustification<Score_> castJustification =
                        (TestConstraintJustification<Score_>) justification;
                softly.assertThat(castJustification.getFacts())
                        .hasSize(4);
                softly.assertThat(constraintMatch.getIndictedObjectList())
                        .hasSize(4);
            });
        }
    }

    @Override
    @TestTemplate
    public void penalizeCustomJustifications() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .penalize(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void penalizeLongCustomJustifications() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .penalizeLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> 2L)
                                .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                .indictWith(List::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void penalizeBigDecimalCustomJustifications() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .penalizeBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(2))
                                        .justifyWith(
                                                (a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                        .indictWith(List::of)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-4)));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void rewardUnweightedCustomJustifications() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .reward(SimpleScore.ONE)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(2));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void rewardCustomJustifications() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .reward(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void rewardLongCustomJustifications() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .rewardLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> 2L)
                                .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                .indictWith(List::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void rewardBigDecimalCustomJustifications() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .rewardBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(2))
                                        .justifyWith(
                                                (a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                        .indictWith(List::of)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(4)));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositiveUnweightedCustomJustifications() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .impact(SimpleScore.ONE)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(2));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositiveCustomJustifications() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .impact(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositiveLongCustomJustifications() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .impactLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> 2L)
                                .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                .indictWith(List::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactPositiveBigDecimalCustomJustifications() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(2))
                                        .justifyWith(
                                                (a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                        .indictWith(List::of)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(4)));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactNegativeCustomJustifications() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector = buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .impact(SimpleScore.ONE, (entity, entity2, value, value2) -> -2)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(-4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactNegativeLongCustomJustifications() {
        TestdataSimpleLongScoreSolution solution = TestdataSimpleLongScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleLongScoreSolution, SimpleLongScore> scoreDirector = buildScoreDirector(
                TestdataSimpleLongScoreSolution.buildSolutionDescriptor(),
                factory -> new Constraint[] {
                        factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                .impactLong(SimpleLongScore.ONE, (entity, entity2, value, value2) -> -2L)
                                .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                .indictWith(List::of)
                                .asConstraint(TEST_CONSTRAINT_NAME)
                });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleLongScore.of(-4));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void impactNegativeBigDecimalCustomJustifications() {
        TestdataSimpleBigDecimalScoreSolution solution = TestdataSimpleBigDecimalScoreSolution.generateSolution();

        InnerScoreDirector<TestdataSimpleBigDecimalScoreSolution, SimpleBigDecimalScore> scoreDirector =
                buildScoreDirector(TestdataSimpleBigDecimalScoreSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachUniquePair(TestdataEntity.class, equal(TestdataEntity::getValue))
                                        .join(TestdataValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                                        .join(TestdataValue.class, equal((entity, entity2, value) -> value, identity()))
                                        .impactBigDecimal(SimpleBigDecimalScore.ONE,
                                                (entity, entity2, value, value2) -> BigDecimal.valueOf(-2))
                                        .justifyWith(
                                                (a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                                        .indictWith(List::of)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleBigDecimalScore.of(BigDecimal.valueOf(-4)));
        assertCustomJustifications(scoreDirector, solution.getEntityList(), solution.getValueList());
    }

    @Override
    @TestTemplate
    public void failWithMultipleJustifications() {
        assertThatCode(() -> buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .penalize(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME)))
                .hasMessageContaining("Maybe the constraint calls justifyWith() twice?");
    }

    @Override
    @TestTemplate
    public void failWithMultipleIndictments() {
        assertThatCode(() -> buildScoreDirector(
                factory -> factory.forEachUniquePair(TestdataLavishEntity.class, equal(TestdataLavishEntity::getValue))
                        .join(TestdataLavishValue.class, equal((entity, entity2) -> entity.getValue(), identity()))
                        .join(TestdataLavishValue.class, equal((entity, entity2, value) -> value, identity()))
                        .penalize(SimpleScore.ONE, (entity, entity2, value, value2) -> 2)
                        .justifyWith((a, b, c, d, score) -> new TestConstraintJustification<>(score, a, b, c, d))
                        .indictWith(List::of)
                        .indictWith(List::of)
                        .asConstraint(TEST_CONSTRAINT_NAME)))
                .hasMessageContaining("Maybe the constraint calls indictWith() twice?");
    }

}
