package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExternalizedListVariableStateSupplyTest {

    @Test
    void initializeRoundTrip() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        try (var supply = new ExternalizedListVariableStateSupply<>(variableDescriptor, notifier)) {

            var v1 = new TestdataAllowsUnassignedValuesListValue("1");
            var v2 = new TestdataAllowsUnassignedValuesListValue("2");
            var v3 = new TestdataAllowsUnassignedValuesListValue("3");
            var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
            var e2 = new TestdataAllowsUnassignedValuesListEntity("e2");

            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(new ArrayList<>(Arrays.asList(e1, e2)));
            solution.setValueList(Arrays.asList(v1, v2, v3));
            var scoreDirector = mock(InnerScoreDirector.class);
            var valueRangeManager =
                    ValueRangeManager.of(variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), solution);
            when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);
            when(scoreDirector.getWorkingSolution()).thenReturn(solution);
            supply.resetWorkingSolution(scoreDirector);

            assertSoftly(softly -> {
                softly.assertThat(supply.getUnassignedCount()).isEqualTo(2);
                softly.assertThat(supply.isAssigned(v1)).isTrue();
                softly.assertThat(supply.isAssigned(v2)).isFalse();
                softly.assertThat(supply.isAssigned(v3)).isFalse();
            });

            verify(notifier).accept(v1);
            verifyNoMoreInteractions(notifier);
            // v2 and v3 are not visited since they are unassigned so their state isn't updated by initialization
        }
    }

    @Test
    void assignRoundTrip() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        try (var supply = new ExternalizedListVariableStateSupply<>(variableDescriptor, notifier)) {

            var v1 = new TestdataAllowsUnassignedValuesListValue("1");
            var v2 = new TestdataAllowsUnassignedValuesListValue("2");
            var v3 = new TestdataAllowsUnassignedValuesListValue("3");
            var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
            var e2 = new TestdataAllowsUnassignedValuesListEntity("e2");

            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(new ArrayList<>(Arrays.asList(e1, e2)));
            solution.setValueList(Arrays.asList(v1, v2, v3));

            var scoreDirector = mock(InnerScoreDirector.class);
            var valueRangeManager =
                    ValueRangeManager.of(variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), solution);
            when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);
            when(scoreDirector.getWorkingSolution()).thenReturn(solution);
            supply.resetWorkingSolution(scoreDirector);

            assertSoftly(softly -> {
                softly.assertThat(supply.getUnassignedCount()).isEqualTo(2);
                softly.assertThat(supply.getElementPosition(v1)).isEqualTo(ElementPosition.of(e1, 0));
                softly.assertThat(supply.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
                softly.assertThat(supply.getElementPosition(v3)).isEqualTo(ElementPosition.unassigned());
            });

            verify(notifier).accept(v1);
            verifyNoMoreInteractions(notifier);
            // v2 and v3 are not visited since they are unassigned so their state isn't updated by initialization
            Mockito.reset(notifier);

            supply.afterListElementUnassigned(scoreDirector, v1);
            assertSoftly(softly -> {
                softly.assertThat(supply.getUnassignedCount()).isEqualTo(3);
                softly.assertThat(supply.getElementPosition(v1)).isEqualTo(ElementPosition.unassigned());
                softly.assertThat(supply.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
                softly.assertThat(supply.getElementPosition(v3)).isEqualTo(ElementPosition.unassigned());
            });
            verify(notifier).accept(v1);
            verifyNoMoreInteractions(notifier);

            // Cannot unassign again.
            assertThatThrownBy(() -> supply.afterListElementUnassigned(scoreDirector, v1))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    /**
     * Replicates {@code ShadowVariableSupport.linkShadowVariables()}'s wiring by hand: finds whichever of the four list shadow
     * variable descriptors are declared on the value class and externalizes them.
     * A value class with all four externalizes fully ({@code requiresPositionMap} becomes {@code false});
     * one with fewer stays partially externalized ({@code requiresPositionMap} stays {@code true}).
     */
    private static <Solution_> void externalizeDeclaredShadowVariables(ExternalizedListVariableStateSupply<Solution_> supply,
            ListVariableDescriptor<Solution_> listVariableDescriptor) {
        var valueEntityDescriptor = listVariableDescriptor.getEntityDescriptor().getSolutionDescriptor()
                .findEntityDescriptor(listVariableDescriptor.getElementType());
        for (var shadow : valueEntityDescriptor.getShadowVariableDescriptors()) {
            switch (shadow) {
                case IndexShadowVariableDescriptor<Solution_> d -> supply.externalize(d);
                case PreviousElementShadowVariableDescriptor<Solution_> d -> supply.externalize(d);
                case NextElementShadowVariableDescriptor<Solution_> d -> supply.externalize(d);
                default -> {
                    // Not a list shadow variable this test cares about.
                }
            }
        }
        var inverseDescriptor = listVariableDescriptor.getInverseRelationShadowVariableDescriptor();
        if (inverseDescriptor != null) {
            supply.externalize(inverseDescriptor);
        }
    }

    @Test
    void changeElementDoesNotRenotifyUnchangedElementWhenAllFourVariablesAreExternalized() {
        // TestdataAllowsUnassignedValuesListValue declares index, inverse, previous and next, so once
        // all four are externalized, requiresPositionMap is false: every piece of position state has a
        // shadow variable event to report it, and the notifier fallback must not fire for an element
        // nothing changed on.
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        try (var supply = new ExternalizedListVariableStateSupply<>(variableDescriptor, notifier)) {
            externalizeDeclaredShadowVariables(supply, variableDescriptor);

            var v1 = new TestdataAllowsUnassignedValuesListValue("1");
            var v2 = new TestdataAllowsUnassignedValuesListValue("2");
            var v3 = new TestdataAllowsUnassignedValuesListValue("3");
            var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1, v2, v3);

            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(new ArrayList<>(List.of(e1)));
            solution.setValueList(Arrays.asList(v1, v2, v3));
            var scoreDirector = mock(InnerScoreDirector.class);
            var valueRangeManager =
                    ValueRangeManager.of(variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), solution);
            when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);
            when(scoreDirector.getWorkingSolution()).thenReturn(solution);
            supply.resetWorkingSolution(scoreDirector);
            Mockito.reset(notifier);

            // Remove v1: v2 and v3 genuinely shift down by one index each. Their shadow variables
            // change, so the events themselves report it - the notifier fallback must stay silent.
            e1.getValueList().remove(v1);
            supply.afterListVariableChanged(scoreDirector, e1, 0, 0);
            assertSoftly(softly -> {
                softly.assertThat(v2.getIndex()).isEqualTo(0);
                softly.assertThat(v3.getIndex()).isEqualTo(1);
            });
            verify(notifier, never()).accept(v2);
            verify(notifier, never()).accept(v3);

            // Re-run the same bracket with no further mutation: the rescan revisits v2 and v3, but neither their position nor their neighbours changed this time (NEITHER, no shadow event fires).
            // Before the fix, the fallback notified them anyway; it must not now.
            Mockito.reset(notifier);
            supply.afterListVariableChanged(scoreDirector, e1, 0, 0);
            verify(notifier, never()).accept(v2);
            verify(notifier, never()).accept(v3);
        }
    }

    @Test
    void addElementDoesNotNotifyAlreadyConsistentElementWhenAllFourVariablesAreExternalized() {
        // Same rationale as changeElementDoesNotRenotifyUnchangedElementWhenAllFourVariablesAreExternalized,
        // but for initialize()'s addElement() rescan: if the shadows already hold the values this rescan
        // would compute anyway (e.g. re-initializing an already-consistent solution), and all four variables
        // are externalized, nothing changed and the notifier fallback must not fire.
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        try (var supply = new ExternalizedListVariableStateSupply<>(variableDescriptor, notifier)) {
            externalizeDeclaredShadowVariables(supply, variableDescriptor);

            var v1 = new TestdataAllowsUnassignedValuesListValue("1");
            var v2 = new TestdataAllowsUnassignedValuesListValue("2");
            var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1, v2);

            // Pre-populate the shadows to the values initialize()'s rescan will compute anyway,
            // simulating a working solution that is already internally consistent (e.g. a clone).
            v1.setIndex(0);
            v1.setEntity(e1);
            v1.setPrevious(null);
            v1.setNext(v2);
            v2.setIndex(1);
            v2.setEntity(e1);
            v2.setPrevious(v1);
            v2.setNext(null);

            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(new ArrayList<>(List.of(e1)));
            solution.setValueList(List.of(v1, v2));
            var scoreDirector = mock(InnerScoreDirector.class);
            var valueRangeManager =
                    ValueRangeManager.of(variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), solution);
            when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);
            when(scoreDirector.getWorkingSolution()).thenReturn(solution);

            supply.resetWorkingSolution(scoreDirector);

            // Nothing about v1/v2 actually changed, so the notifier fallback must stay silent.
            verify(notifier, never()).accept(v1);
            verify(notifier, never()).accept(v2);
        }
    }

    @Test
    void changeElementStillNotifiesUnchangedElementWhenNotAllVariablesAreExternalized() {
        // TestdataListValue declares only index and inverse - there is no previous/next shadow
        // variable to externalize at all, so requiresPositionMap stays true: the notifier remains the
        // only signal for state that has no shadow variable to report it, and must keep firing even
        // when nothing else changed.
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        @SuppressWarnings("unchecked")
        var notifier = (Consumer<Object>) mock(Consumer.class);
        try (var supply = new ExternalizedListVariableStateSupply<>(variableDescriptor, notifier)) {
            externalizeDeclaredShadowVariables(supply, variableDescriptor);

            var v1 = new TestdataListValue("1");
            var v2 = new TestdataListValue("2");
            var v3 = new TestdataListValue("3");
            var e1 = new TestdataListEntity("e1", v1, v2, v3);

            var solution = new TestdataListSolution();
            solution.setEntityList(new ArrayList<>(List.of(e1)));
            solution.setValueList(Arrays.asList(v1, v2, v3));
            var scoreDirector = mock(InnerScoreDirector.class);
            var valueRangeManager =
                    ValueRangeManager.of(variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), solution);
            when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);
            when(scoreDirector.getWorkingSolution()).thenReturn(solution);
            supply.resetWorkingSolution(scoreDirector);
            Mockito.reset(notifier);

            // Re-run the bracket for the last element with no mutation at all: index and entity are both unchanged (NEITHER), and there is no previous/next processor to report it either way.
            // With nothing externalized to tell Neighborhoods about it, the notifier must fire.
            supply.afterListVariableChanged(scoreDirector, e1, 2, 2);
            verify(notifier).accept(v3);
        }
    }

}
