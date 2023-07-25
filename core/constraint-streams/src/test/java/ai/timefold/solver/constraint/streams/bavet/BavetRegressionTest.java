package ai.timefold.solver.constraint.streams.bavet;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamTest;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishExtra;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.mockito.internal.util.Supplier;

import java.util.Objects;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;

class BavetRegressionTest extends AbstractConstraintStreamTest {

    protected BavetRegressionTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

    @TestTemplate
    public void filteringJoinNullConflict() { // See https://github.com/TimefoldAI/timefold-solver/issues/186.
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .join(TestdataEntity.class,
                                                filtering((a, b) -> {
                                                    if (a.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of A is null even though forEach() should have eliminated it.");
                                                    }
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity2)); // Only entity1 is left, because forEach/join ignore nulls.

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector);
    }

    @TestTemplate
    public void filteringIfExistsNullConflict() { // See https://github.com/TimefoldAI/timefold-solver/issues/186.
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .ifExists(TestdataEntity.class,
                                                filtering((a, b) -> {
                                                    if (a.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of A is null even though forEach() should have eliminated it.");
                                                    }
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2)); // Only entity1 is left, because forEach/ifExists ignore nulls.

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1));
    }
}
