package ai.timefold.solver.core.impl.neighborhood.bias;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.move.DefaultMoveTestContext;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsBasedMoveRepository;
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

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * End-to-end regression for {@code RandomOrderNeighborhoodIterator}'s per-move (not per-step) draw,
 * through two or three real {@link MoveProvider}s registered on one repository, the same way
 * {@link NeighborhoodsBasedMoveRepository} is used in production. Before the fix, only the
 * neighborhood drawn first on the first exhaustion check ever produced a move; every other
 * configured neighborhood was starved for the whole run. Each real neighborhood's move iterator is
 * endless (it never reports exhaustion on its own), which is exactly the property that made the
 * old per-exhaustion draw fail: a per-step draw over endless children only ever picks the first one
 * drawn.
 */
class NeighborhoodMixingBiasIT extends AbstractBiasIT {

    private static final int DRAW_COUNT = 200_000;
    private static final int ENTITIES_PER_NEIGHBORHOOD = 5;

    @ValueSource(ints = { 2, 3 })
    @ParameterizedTest
    void drawsAreUniformOverNeighborhoods(int neighborhoodCount) {
        var variable = TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);
        var prefixList = neighborhoodCount == 2 ? List.of("a", "b") : List.of("a", "b", "c");

        var entityList = new ArrayList<TestdataEntity>();
        for (var prefix : prefixList) {
            entityList.addAll(entities(prefix, ENTITIES_PER_NEIGHBORHOOD));
        }
        var valueList = new ArrayList<TestdataValue>();
        for (var i = 0; i < ENTITIES_PER_NEIGHBORHOOD; i++) {
            valueList.add(new TestdataValue("v" + i));
        }
        var solution = new TestdataSolution("solution");
        solution.setEntityList(entityList);
        solution.setValueList(valueList);

        var moveTester = MoveTester.build(TestdataSolution.buildMetaModel());
        var moveTestContext = (DefaultMoveTestContext<TestdataSolution>) moveTester.using(solution);
        var scoreDirector = moveTestContext.getScoreDirector();

        var moveStreamFactory =
                new DefaultMoveStreamFactory<TestdataSolution>(TestdataSolution.buildSolutionDescriptor(),
                        EnvironmentMode.FULL_ASSERT);
        var providerList = prefixList.stream()
                .<MoveProvider<TestdataSolution>> map(prefix -> new PickEntitiesStartingWith(variable, prefix))
                .toList();
        var repository = new NeighborhoodsBasedMoveRepository<>(moveStreamFactory, providerList);

        var solverScope = new SolverScope<TestdataSolution>();
        solverScope.setWorkingRandom(RandomSource.seeded(0L));
        solverScope.setScoreDirector(scoreDirector);
        repository.solvingStarted(solverScope);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        repository.phaseStarted(phaseScope);

        var iterator = repository.iterator(RandomSource.seeded(0L).moveIteratorUsage());
        tally("%d-way neighborhood mixing".formatted(neighborhoodCount), DRAW_COUNT, draw -> {
            var move = iterator.next();
            var entity = (TestdataEntity) move.getPlanningEntities().getFirst();
            return entity.getCode().substring(0, 1);
        }).expectUniform(prefixList).assertWithinSigma(SIGMA_LIMIT);
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
