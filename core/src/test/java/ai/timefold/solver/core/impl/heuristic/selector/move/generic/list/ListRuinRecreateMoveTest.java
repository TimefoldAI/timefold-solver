package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.ListRuinRecreateMove;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ListRuinRecreateMoveTest {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void rebase() {
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();

        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var e1 = new TestdataListEntity("e1", v1);
        var e2 = new TestdataListEntity("e2");
        var e3 = new TestdataListEntity("e3", v1);

        var destinationV1 = new TestdataListValue("v1");
        var destinationV2 = new TestdataListValue("v2");
        var destinationE1 = new TestdataListEntity("e1", destinationV1);
        var destinationE2 = new TestdataListEntity("e2");
        var destinationE3 = new TestdataListEntity("e3", destinationV1);

        var destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                });

        var move = new ListRuinRecreateMove<TestdataListSolution>(mock(ListVariableDescriptor.class),
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), Arrays.asList(v1, v2),
                Set.of(e1, e2, e3));
        var rebasedMove = move.rebase(destinationScoreDirector);

        assertSoftly(softly -> {
            softly.assertThat((Collection) rebasedMove.getPlanningEntities())
                    .containsExactlyInAnyOrder(destinationE1, destinationE2, destinationE3); // The input set is not ordered.
            softly.assertThat((Collection) rebasedMove.getPlanningValues())
                    .containsExactly(destinationV1, destinationV2);
        });

    }

    @SuppressWarnings("unchecked")
    @Test
    void equality() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var e1 = new TestdataListEntity("e1", v1);
        var e2 = new TestdataListEntity("e2");

        var descriptor = mock(ListVariableDescriptor.class);
        var move = new ListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v1));
        var sameMove = new ListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v1));
        assertThat(move).isEqualTo(sameMove);

        var differentMove = new ListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v2));
        assertThat(move).isNotEqualTo(differentMove);

        var anotherDifferentMove = new ListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e2),
                Set.of(v1));
        assertThat(move).isNotEqualTo(anotherDifferentMove);

        var yetAnotherDifferentMove = new ListRuinRecreateMove<TestdataListSolution>(mock(ListVariableDescriptor.class),
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v1));
        assertThat(move).isNotEqualTo(yetAnotherDifferentMove);
    }

}
