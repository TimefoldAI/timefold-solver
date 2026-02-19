package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.preview.api.move.Move;

final class PlacementAssertions {

    static <Solution_> void assertEntityPlacement(Placement<Solution_> placement, String entityCode, String... valueCodes) {
        var iterator = placement.iterator();
        assertThat(iterator).isNotNull();
        for (var valueCode : valueCodes) {
            assertThat(iterator).hasNext();
            var move = adapt(iterator.next());
            assertCode(entityCode, move.getEntity());
            assertCode(valueCode, move.getToPlanningValue());
        }
        assertThat(iterator).isExhausted();
    }

    static <Solution_> SelectorBasedChangeMove<Solution_> adapt(Move<Solution_> move) {
        return (SelectorBasedChangeMove<Solution_>) move;
    }

    static <Solution_> void assertValuePlacement(Placement<Solution_> placement, String valueCode, String... entityCodes) {
        var iterator = placement.iterator();
        assertThat(iterator).isNotNull();
        for (var entityCode : entityCodes) {
            assertThat(iterator).hasNext();
            var move = adapt(iterator.next());
            assertCode(entityCode, move.getEntity());
            assertCode(valueCode, move.getToPlanningValue());
        }
        assertThat(iterator).isExhausted();
    }

    private PlacementAssertions() {
    }
}
