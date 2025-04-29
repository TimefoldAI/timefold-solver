package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

class RuinRecreateMoveTest {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void rebase() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", null);
        var e3 = new TestdataEntity("e3", v1);

        var destinationV1 = new TestdataValue("v1");
        var destinationV2 = new TestdataValue("v2");
        var destinationE1 = new TestdataEntity("e1", destinationV1);
        var destinationE2 = new TestdataEntity("e2", null);
        var destinationE3 = new TestdataEntity("e3", destinationV1);

        var destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                });

        var move = new RuinRecreateMove<TestdataSolution>(mock(GenuineVariableDescriptor.class),
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), Arrays.asList(e1, e2, e3),
                Set.of(v1, v2));
        var rebasedMove = move.rebase(destinationScoreDirector);

        assertSoftly(softly -> {
            softly.assertThat((Collection) rebasedMove.getPlanningEntities())
                    .containsExactly(destinationE1, destinationE2, destinationE3);
            softly.assertThat((Collection) rebasedMove.getPlanningValues())
                    .containsExactlyInAnyOrder(destinationV1, destinationV2); // The input set is not ordered.
        });

    }

    @SuppressWarnings("unchecked")
    @Test
    void equality() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", null);

        var descriptor = mock(GenuineVariableDescriptor.class);
        var move = new RuinRecreateMove<TestdataSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v1));
        var sameMove = new RuinRecreateMove<TestdataSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v1));
        assertThat(move).isEqualTo(sameMove);

        var differentMove = new RuinRecreateMove<TestdataSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v2));
        assertThat(move).isNotEqualTo(differentMove);

        var anotherDifferentMove = new RuinRecreateMove<TestdataSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e2),
                Set.of(v1));
        assertThat(move).isNotEqualTo(anotherDifferentMove);

        var yetAnotherDifferentMove = new RuinRecreateMove<TestdataSolution>(mock(GenuineVariableDescriptor.class),
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mock(SolverScope.class), List.of(e1),
                Set.of(v1));
        assertThat(move).isNotEqualTo(yetAnotherDifferentMove);
    }
}
