package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.move.DefaultMoveTestContext;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.assertj.core.data.Offset;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * End-to-end regression for the {@code RandomOrderNeighborhoodIterator} fix, using two real
 * {@link MoveProvider}s registered on one repository, the same way {@link NeighborhoodsBasedMoveRepository}
 * is used in production. Before the fix, only the neighborhood drawn first on the first exhaustion check ever
 * produces a move; every other configured neighborhood is starved for the whole run.
 */
@Execution(ExecutionMode.CONCURRENT)
class RandomOrderNeighborhoodIteratorMixingIT {

    private static final int DRAW_COUNT = 100_000;

    @Test
    void bothNeighborhoodsProduceMovesWithRoughlyEqualShare() {
        var variable = TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var aEntityList = entities("a", 5);
        var bEntityList = entities("b", 5);
        var valueList = new ArrayList<TestdataValue>();
        for (var i = 0; i < 5; i++) {
            valueList.add(new TestdataValue("v" + i));
        }
        var solution = new TestdataSolution("solution");
        var entityList = new ArrayList<TestdataEntity>();
        entityList.addAll(aEntityList);
        entityList.addAll(bEntityList);
        solution.setEntityList(entityList);
        solution.setValueList(valueList);

        var moveTester = MoveTester.build(TestdataSolution.buildMetaModel());
        var moveTestContext = (DefaultMoveTestContext<TestdataSolution>) moveTester.using(solution);
        var scoreDirector = moveTestContext.getScoreDirector();

        var moveStreamFactory =
                new DefaultMoveStreamFactory<TestdataSolution>(TestdataSolution.buildSolutionDescriptor(),
                        EnvironmentMode.FULL_ASSERT);
        var repository = new NeighborhoodsBasedMoveRepository<>(moveStreamFactory,
                List.of(new PickEntitiesStartingWith(variable, "a"), new PickEntitiesStartingWith(variable, "b")));

        var solverScope = new SolverScope<TestdataSolution>();
        solverScope.setWorkingRandom(RandomSource.seeded(0L));
        solverScope.setScoreDirector(scoreDirector);
        repository.solvingStarted(solverScope);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        repository.phaseStarted(phaseScope);

        var iterator = repository.iterator(RandomSource.seeded(0L).moveIteratorUsage());
        var aCount = 0;
        var bCount = 0;
        for (var i = 0; i < DRAW_COUNT; i++) {
            var move = iterator.next();
            var entity = (TestdataEntity) move.getPlanningEntities().getFirst();
            if (entity.getCode().startsWith("a")) {
                aCount++;
            } else if (entity.getCode().startsWith("b")) {
                bCount++;
            }
        }

        // Both neighborhoods must be reachable at all, and with a roughly equal share (2% tolerance).
        var expectedCount = DRAW_COUNT / 2.0;
        var threshold = expectedCount * 0.02;
        assertThat(aCount).isCloseTo((int) expectedCount, Offset.offset((int) threshold));
        assertThat(bCount).isCloseTo((int) expectedCount, Offset.offset((int) threshold));
    }

    private static List<TestdataEntity> entities(String prefix, int count) {
        var entityList = new ArrayList<TestdataEntity>(count);
        for (var i = 0; i < count; i++) {
            entityList.add(new TestdataEntity(prefix + i));
        }
        return entityList;
    }

    /**
     * A single neighborhood: picks any entity whose code starts with {@code prefix}, and any value.
     */
    @NullMarked
    private record PickEntitiesStartingWith(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable,
            String prefix) implements MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false)
                    .filter((solutionView, entity) -> entity.getCode().startsWith(prefix));
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

}
