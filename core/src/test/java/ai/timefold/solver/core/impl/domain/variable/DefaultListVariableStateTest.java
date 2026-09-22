package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.change.MockProblemChangeDirector;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultListVariableStateTest {

    @Test
    void initializeRoundTrip() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);

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
        state.resetWorkingSolution(scoreDirector);

        assertSoftly(softly -> {
            softly.assertThat(state.getUnassignedCount()).isEqualTo(2);
            softly.assertThat(state.isAssigned(v1)).isTrue();
            softly.assertThat(state.isAssigned(v2)).isFalse();
            softly.assertThat(state.isAssigned(v3)).isFalse();
        });

        verify(notifier).accept(v1);
        verifyNoMoreInteractions(notifier);
        // v2 and v3 are not visited since they are unassigned so their state isn't updated
        // by initialization
    }

    @Test
    void assignRoundTrip() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);

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
        state.resetWorkingSolution(scoreDirector);

        assertSoftly(softly -> {
            softly.assertThat(state.getUnassignedCount()).isEqualTo(2);
            softly.assertThat(state.getElementPosition(v1)).isEqualTo(ElementPosition.of(e1, 0));
            softly.assertThat(state.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
            softly.assertThat(state.getElementPosition(v3)).isEqualTo(ElementPosition.unassigned());
        });

        verify(notifier).accept(v1);
        verifyNoMoreInteractions(notifier);
        // v2 and v3 are not visited since they are unassigned so their state isn't updated
        // by initialization
        Mockito.reset(notifier);

        state.afterListElementUnassigned(scoreDirector, v1);
        assertSoftly(softly -> {
            softly.assertThat(state.getUnassignedCount()).isEqualTo(3);
            softly.assertThat(state.getElementPosition(v1)).isEqualTo(ElementPosition.unassigned());
            softly.assertThat(state.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
            softly.assertThat(state.getElementPosition(v3)).isEqualTo(ElementPosition.unassigned());
        });
        verify(notifier).accept(v1);
        verifyNoMoreInteractions(notifier);

        // Cannot unassign again.
        assertThatThrownBy(() -> state.afterListElementUnassigned(scoreDirector, v1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getInverseSingletonAgreesWithGetElementPosition() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);

        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1, v2));
        resetOn(state, variableDescriptor, solution);

        var v1Entity = state.getElementPosition(v1).ensureAssigned().entity();
        assertSoftly(softly -> {
            softly.assertThat(state.getInverseSingleton(v1)).isEqualTo(e1);
            softly.assertThat(v1Entity).isEqualTo(e1);
            softly.assertThat(state.getInverseSingleton(v2)).isNull();
            softly.assertThat(state.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
        });
    }

    @Test
    void getInverseSingletonAgreesWithGetElementPositionWhenInverseIsExternalized() {
        // Only the inverse relation is externalized; index/previous/next stay internal so
        // requiresPositionMap stays true. getElementPosition then reads the position map while
        // getInverseSingleton reads the externalized shadow field directly - two different stores
        // that must still agree.
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        state.externalize(variableDescriptor.getInverseRelationShadowVariableDescriptor());

        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1, v2));
        resetOn(state, variableDescriptor, solution);

        var v1Entity = state.getElementPosition(v1).ensureAssigned().entity();
        assertSoftly(softly -> {
            softly.assertThat(state.getInverseSingleton(v1)).isEqualTo(e1);
            softly.assertThat(v1Entity).isEqualTo(e1);
            softly.assertThat(state.getInverseSingleton(v2)).isNull();
            softly.assertThat(state.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
        });
    }

    @Test
    void getInverseSingletonPreservesExternalizedInverseOnFirstResetEvenWhenShadowsAreExpectedCorrect() {
        // Mirrors AbstractConstraintAssertion.ensureInitialized():
        // manually set shadow fields on unassigned values (v2 not in e1's value list)
        // must be preserved on the first reset rather than corrected.
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        state.externalize(variableDescriptor.getInverseRelationShadowVariableDescriptor());

        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
        v2.setEntity(e1); // Hand-set by the caller, deliberately not mirrored into e1's value list.

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1, v2));
        resetOn(state, variableDescriptor, solution, true);

        assertSoftly(softly -> {
            softly.assertThat(state.getElementPosition(v2)).isEqualTo(ElementPosition.unassigned());
            softly.assertThat(state.getInverseSingleton(v2)).isEqualTo(e1);
        });
    }

    @Test
    void getInverseSingletonClearsStaleExternalizedInverseOnSubsequentResetEvenWhenShadowsAreExpectedCorrect() {
        // MockProblemChangeDirector.updateShadowVariables() is a no-op, so v1's stale inverse field stays.
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        state.externalize(variableDescriptor.getInverseRelationShadowVariableDescriptor());

        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1));
        resetOn(state, variableDescriptor, solution, false); // First reset: mirrors solve start.

        ProblemChange<TestdataAllowsUnassignedValuesListSolution> removeV1 =
                (workingSolution, problemChangeDirector) -> problemChangeDirector.changeVariable(e1, "valueList",
                        entity -> entity.getValueList().remove(v1));
        removeV1.doChange(solution, new MockProblemChangeDirector());
        resetOn(state, variableDescriptor, solution, true); // Second reset: mirrors a problem change.

        assertSoftly(softly -> {
            softly.assertThat(state.getElementPosition(v1)).isEqualTo(ElementPosition.unassigned());
            softly.assertThat(state.getInverseSingleton(v1)).isNull();
        });
    }

    /**
     * Replicates {@code VariableSupport.linkShadowVariables()}'s wiring by hand: finds whichever of the four list shadow
     * variable descriptors are declared on the value class and externalizes them.
     * A value class with all four externalizes fully ({@code requiresPositionMap} becomes {@code false});
     * one with fewer stays partially externalized ({@code requiresPositionMap} stays {@code true}).
     */
    private static <Solution_> void externalizeDeclaredShadowVariables(DefaultListVariableState<Solution_> state,
            ListVariableDescriptor<Solution_> listVariableDescriptor) {
        var valueEntityDescriptor = listVariableDescriptor.getEntityDescriptor().getSolutionDescriptor()
                .findEntityDescriptor(listVariableDescriptor.getElementType());
        for (var shadow : valueEntityDescriptor.getShadowVariableDescriptors()) {
            switch (shadow) {
                case IndexShadowVariableDescriptor<Solution_> d -> state.externalize(d);
                case PreviousElementShadowVariableDescriptor<Solution_> d -> state.externalize(d);
                case NextElementShadowVariableDescriptor<Solution_> d -> state.externalize(d);
                default -> {
                    // Not a list shadow variable this test cares about.
                }
            }
        }
        var inverseDescriptor = listVariableDescriptor.getInverseRelationShadowVariableDescriptor();
        if (inverseDescriptor != null) {
            state.externalize(inverseDescriptor);
        }
    }

    @Test
    void changeElementDoesNotRenotifyUnchangedElementWhenAllFourVariablesAreExternalized() {
        // With all four shadow variables externalized (requiresPositionMap is false),
        // the notifier fallback must not fire for unchanged elements.
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        externalizeDeclaredShadowVariables(state, variableDescriptor);

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
        state.resetWorkingSolution(scoreDirector);
        Mockito.reset(notifier);

        // Remove v1: v2 and v3 genuinely shift down by one index each. Their shadow variables
        // change, so the events themselves report it - the notifier fallback must stay silent.
        e1.getValueList().remove(v1);
        state.afterListVariableChanged(scoreDirector, e1, 0, 0);
        assertSoftly(softly -> {
            softly.assertThat(v2.getIndex()).isEqualTo(0);
            softly.assertThat(v3.getIndex()).isEqualTo(1);
        });
        verify(notifier, never()).accept(v2);
        verify(notifier, never()).accept(v3);

        // Re-run the same bracket with no further mutation: the rescan revisits v2 and v3, but neither their position nor their neighbours changed this time (NEITHER, no shadow event fires).
        // Before the fix, the fallback notified them anyway; it must not now.
        Mockito.reset(notifier);
        state.afterListVariableChanged(scoreDirector, e1, 0, 0);
        verify(notifier, never()).accept(v2);
        verify(notifier, never()).accept(v3);
    }

    @Test
    void addElementDoesNotNotifyAlreadyConsistentElementWhenAllFourVariablesAreExternalized() {
        // When all four shadow variables are externalized and already consistent upon initialization,
        // no state changes occur and the notifier fallback must not fire.
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        externalizeDeclaredShadowVariables(state, variableDescriptor);

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

        state.resetWorkingSolution(scoreDirector);

        // Nothing about v1/v2 actually changed, so the notifier fallback must stay silent.
        verify(notifier, never()).accept(v1);
        verify(notifier, never()).accept(v2);
    }

    @Test
    void changeElementStillNotifiesUnchangedElementWhenNotAllVariablesAreExternalized() {
        // TestdataListValue lacks previous/next shadows, keeping requiresPositionMap true;
        // the notifier remains the fallback signal and must fire even when unchanged.
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var supply = new DefaultListVariableState<>(variableDescriptor, notifier);
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

    /**
     * Builds a mock score director over the given solution and hands it to the supply,
     * matching what the solver does before any element state is queried.
     */
    private static <Solution_> InnerScoreDirector<Solution_, ?> resetOn(DefaultListVariableState<Solution_> state,
            ListVariableDescriptor<Solution_> variableDescriptor, Solution_ solution) {
        return resetOn(state, variableDescriptor, solution, false);
    }

    /**
     * As {@link #resetOn(DefaultListVariableState, ListVariableDescriptor, Object)},
     * but with control over {@link InnerScoreDirector#expectShadowVariablesInCorrectState()} -
     * true simulates a problem-change reset (shadows asserted already correct),
     * false simulates a fresh solve start (shadows rebuilt from scratch).
     */
    private static <Solution_> InnerScoreDirector<Solution_, ?> resetOn(DefaultListVariableState<Solution_> state,
            ListVariableDescriptor<Solution_> variableDescriptor, Solution_ solution,
            boolean expectShadowVariablesInCorrectState) {
        var scoreDirector = (InnerScoreDirector<Solution_, ?>) mock(InnerScoreDirector.class);
        var valueRangeManager =
                ValueRangeManager.of(variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), solution);
        when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        when(scoreDirector.expectShadowVariablesInCorrectState()).thenReturn(expectShadowVariablesInCorrectState);
        state.resetWorkingSolution(scoreDirector);
        return scoreDirector;
    }

    @Test
    void isPinnedFollowsThePinIndexBoundary() {
        var variableDescriptor = TestdataPinnedWithIndexListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var e1 = new TestdataPinnedWithIndexListEntity("e1", new ArrayList<>(List.of(v1, v2, v3)));
        e1.setPinIndex(2);

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1, v2, v3));
        resetOn(state, variableDescriptor, solution);

        assertSoftly(softly -> {
            softly.assertThat(state.isPinned(v1)).isTrue();
            softly.assertThat(state.isPinned(v2)).isTrue();
            softly.assertThat(state.isPinned(v3)).isFalse();
        });
    }

    @Test
    void isPinnedIsTrueForEveryElementOfAFullyPinnedEntity() {
        var variableDescriptor = TestdataPinnedWithIndexListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        // The entity is pinned as a whole, so the pin index never gets consulted.
        var e1 = new TestdataPinnedWithIndexListEntity("e1", new ArrayList<>(List.of(v1, v2)));
        e1.setPinned(true);
        e1.setPinIndex(0);

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1, v2));
        resetOn(state, variableDescriptor, solution);

        assertSoftly(softly -> {
            softly.assertThat(state.isPinned(v1)).isTrue();
            softly.assertThat(state.isPinned(v2)).isTrue();
        });
    }

    @Test
    void isPinnedIsFalseForAnUnassignedElement() {
        var variableDescriptor = TestdataPinnedWithIndexListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        // v2 belongs to no entity, so it has no position at all.
        var e1 = new TestdataPinnedWithIndexListEntity("e1", new ArrayList<>(List.of(v1)));
        e1.setPinIndex(1);

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1, v2));
        resetOn(state, variableDescriptor, solution);

        assertSoftly(softly -> {
            softly.assertThat(state.isAssigned(v2)).isFalse();
            softly.assertThat(state.isPinned(v2)).isFalse();
            softly.assertThat(state.isPinned(v1)).isTrue();
        });
    }

    @Test
    void isPinnedIsFalseWhenTheDescriptorDoesNotSupportPinning() {
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        var v1 = new TestdataListValue("1");

        // Asked before the working solution is known: the unsupported-pinning guard must answer
        // without reaching for the solution, rather than throwing.
        assertThat(state.isPinned(v1)).isFalse();

        var e1 = new TestdataListEntity("e1", new ArrayList<>(List.of(v1)));
        var solution = new TestdataListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1));
        resetOn(state, variableDescriptor, solution);

        assertThat(state.isPinned(v1)).isFalse();
    }

    @Test
    void isPinnedFollowsAnElementAsItMoves() {
        var variableDescriptor = TestdataPinnedWithIndexListEntity.buildVariableDescriptorForValueList();
        var notifier = (Consumer<Object>) mock(Consumer.class);
        var state = new DefaultListVariableState<>(variableDescriptor, notifier);
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var e1 = new TestdataPinnedWithIndexListEntity("e1", new ArrayList<>(List.of(v1, v2)));
        e1.setPinIndex(1);

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(new ArrayList<>(List.of(e1)));
        solution.setValueList(List.of(v1, v2));

        var scoreDirector = resetOn(state, variableDescriptor, solution);

        assertSoftly(softly -> {
            softly.assertThat(state.isPinned(v1)).isTrue();
            softly.assertThat(state.isPinned(v2)).isFalse();
        });

        // Swapping the two elements swaps which one sits below the pin index.
        state.beforeListVariableChanged(scoreDirector, e1, 0, 2);
        Collections.swap(e1.getValueList(), 0, 1);
        state.afterListVariableChanged(scoreDirector, e1, 0, 2);

        assertSoftly(softly -> {
            softly.assertThat(state.isPinned(v1)).isFalse();
            softly.assertThat(state.isPinned(v2)).isTrue();
        });
    }

}
