package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KOptListMovePatchingTest {

    /**
     * Forces the cycle that patching picks; the list has 12 values, so no other draw has bound 2.
     */
    private static final class ForcedCycleRandom extends Random {

        private final int cycle;
        private int cycleDrawCount = 0;

        ForcedCycleRandom(long seed, int cycle) {
            super(seed);
            this.cycle = cycle;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 2) {
                cycleDrawCount++;
                return cycle;
            }
            return super.nextInt(bound);
        }
    }

    @SuppressWarnings("unchecked")
    private static IterableValueSelector<Object> randomValueSelector(List<Object> values, Random random) {
        var valueSelector = (IterableValueSelector<Object>) mock(IterableValueSelector.class);
        when(valueSelector.iterator()).thenAnswer(invocation -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Object next() {
                return values.get(random.nextInt(values.size()));
            }
        });
        return valueSelector;
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void patchesEitherCycle(int cycle) {
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());
        var values = new ArrayList<TestdataListValue>();
        for (var i = 0; i < 12; i++) {
            values.add(new TestdataListValue("v" + i));
        }
        var entity = new TestdataListEntity("e", new ArrayList<>(values));
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(values);
        SolutionManager.updateShadowVariables(solution);
        scoreDirector.setWorkingSolution(solution);

        var random = new ForcedCycleRandom(0, cycle);
        var valueSelectorRandom = new Random(1);
        List<Object> valueObjects = new ArrayList<>(values);
        // Always picks k = 4; with maximumK = 6, every 6-opt move is a patched 4-opt move.
        var iterator = new KOptListMoveIterator<TestdataListSolution, Object>(random, variableDescriptor,
                scoreDirector.getListVariableState(variableDescriptor), randomValueSelector(valueObjects, valueSelectorRandom),
                randomValueSelector(valueObjects, valueSelectorRandom), 4, 6, new int[] { 1, 0, 0 });
        SelectorBasedKOptListMove<TestdataListSolution> patchedMove = null;
        for (var i = 0; i < 10_000 && patchedMove == null; i++) {
            if (iterator.next() instanceof SelectorBasedKOptListMove<TestdataListSolution> kOptListMove
                    && kOptListMove.getDescriptor().k() == 6) {
                patchedMove = kOptListMove;
            }
        }
        assertThat(patchedMove).isNotNull();
        assertThat(random.cycleDrawCount).isPositive();

        patchedMove.execute(scoreDirector);
        assertThat(entity.getValueList())
                .containsExactlyInAnyOrderElementsOf(values)
                .isNotEqualTo(values);
    }

}
