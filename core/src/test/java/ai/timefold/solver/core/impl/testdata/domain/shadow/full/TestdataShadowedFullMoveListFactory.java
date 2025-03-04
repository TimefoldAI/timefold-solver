package ai.timefold.solver.core.impl.testdata.domain.shadow.full;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;

public class TestdataShadowedFullMoveListFactory implements MoveListFactory<TestdataShadowedFullSolution> {
    @Override
    public List<? extends Move<TestdataShadowedFullSolution>> createMoveList(TestdataShadowedFullSolution solution) {
        var out = new ArrayList<Move<TestdataShadowedFullSolution>>();
        var random = new Random(0);
        for (var a : solution.entityList) {
            for (var b : solution.entityList) {
                if (a != b) {
                    out.add(new TestdataShadowedFullMultiTailSwapListMove(a, b, random.nextLong()));
                }
            }
        }
        return out;
    }
}
