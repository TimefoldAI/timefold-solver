package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getEntityDescriptor;
import static ai.timefold.solver.core.testutil.PlannerAssert.DO_NOT_ASSERT_SIZE;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfNeverEndingIterableSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.FromSolutionEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntityByEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicRecordingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicReplayingEntitySelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class SwapMoveSelectorTest {

    @Test
    void originalLeftEqualsRight() {
        EntitySelector entitySelector = SelectorTestUtils.mockEntitySelector(TestdataEntity.buildEntityDescriptor(),
                new TestdataEntity("a"), new TestdataEntity("b"), new TestdataEntity("c"), new TestdataEntity("d"));

        SwapMoveSelector moveSelector = new SwapMoveSelector(entitySelector, entitySelector,
                entitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), false);

        SolverScope solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        assertAllCodesOfMoveSelector(moveSelector, "a<->b", "a<->c", "a<->d", "b<->c", "b<->d", "c<->d");
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        assertAllCodesOfMoveSelector(moveSelector, "a<->b", "a<->c", "a<->d", "b<->c", "b<->d", "c<->d");
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        assertAllCodesOfMoveSelector(moveSelector, "a<->b", "a<->c", "a<->d", "b<->c", "b<->d", "c<->d");
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        assertAllCodesOfMoveSelector(moveSelector, "a<->b", "a<->c", "a<->d", "b<->c", "b<->d", "c<->d");
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        assertAllCodesOfMoveSelector(moveSelector, "a<->b", "a<->c", "a<->d", "b<->c", "b<->d", "c<->d");
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 2, 5);
    }

    @Test
    void emptyOriginalLeftEqualsRight() {
        EntitySelector entitySelector = SelectorTestUtils.mockEntitySelector(TestdataEntity.buildEntityDescriptor());

        SwapMoveSelector moveSelector = new SwapMoveSelector(entitySelector, entitySelector,
                entitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), false);

        SolverScope solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 2, 5);
    }

    @Test
    void originalLeftUnequalsRight() {
        EntityDescriptor entityDescriptor = TestdataEntity.buildEntityDescriptor();

        EntitySelector leftEntitySelector = SelectorTestUtils.mockEntitySelector(entityDescriptor,
                new TestdataEntity("a"), new TestdataEntity("b"), new TestdataEntity("c"), new TestdataEntity("d"));

        EntitySelector rightEntitySelector = SelectorTestUtils.mockEntitySelector(entityDescriptor,
                new TestdataEntity("x"), new TestdataEntity("y"), new TestdataEntity("z"));

        SwapMoveSelector moveSelector = new SwapMoveSelector(leftEntitySelector, rightEntitySelector,
                leftEntitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), false);

        SolverScope solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        assertAllCodesOfMoveSelector(moveSelector,
                "a<->x", "a<->y", "a<->z", "b<->x", "b<->y", "b<->z",
                "c<->x", "c<->y", "c<->z", "d<->x", "d<->y", "d<->z");
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        assertAllCodesOfMoveSelector(moveSelector,
                "a<->x", "a<->y", "a<->z", "b<->x", "b<->y", "b<->z",
                "c<->x", "c<->y", "c<->z", "d<->x", "d<->y", "d<->z");
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        assertAllCodesOfMoveSelector(moveSelector,
                "a<->x", "a<->y", "a<->z", "b<->x", "b<->y", "b<->z",
                "c<->x", "c<->y", "c<->z", "d<->x", "d<->y", "d<->z");
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        assertAllCodesOfMoveSelector(moveSelector,
                "a<->x", "a<->y", "a<->z", "b<->x", "b<->y", "b<->z",
                "c<->x", "c<->y", "c<->z", "d<->x", "d<->y", "d<->z");
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        assertAllCodesOfMoveSelector(moveSelector,
                "a<->x", "a<->y", "a<->z", "b<->x", "b<->y", "b<->z",
                "c<->x", "c<->y", "c<->z", "d<->x", "d<->y", "d<->z");
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(leftEntitySelector, 1, 2, 5);
        verifyPhaseLifecycle(rightEntitySelector, 1, 2, 5);
    }

    @Test
    void originalLeftUnequalsRightWithEntityRange() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var e1 = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v4));
        var e2 = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        var e3 = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v1, v4));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var leftEntitySelector =
                new FromSolutionEntitySelector<>(getEntityDescriptor(scoreDirector), SelectionCacheType.JUST_IN_TIME, false);
        var entityMimicRecorder = new MimicRecordingEntitySelector<>(leftEntitySelector);

        var replayingEntitySelector = new MimicReplayingEntitySelector<>(entityMimicRecorder);
        var rightEntitySelector =
                new FilteringEntityByEntitySelector<>(leftEntitySelector, replayingEntitySelector, false);

        // It is impossible for the left and right selectors to be equal
        // when using the entity-range filtering node FilteringEntityByEntitySelector
        var moveSelector = new SwapMoveSelector<>(entityMimicRecorder, rightEntitySelector,
                leftEntitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        scoreDirector.setWorkingSolution(solution);
        var expectedSize = (long) solution.getEntityList().size() * solution.getEntityList().size();

        // The moves are duplicated because the left and right selectors are not equal,
        // and listIterator(index) is not called in such cases.
        assertAllCodesOfMoveSelector(moveSelector, expectedSize, "A<->B",
                "A<->C",
                "B<->A",
                "B<->C",
                "C<->A",
                "C<->B");
    }

    @Test
    void emptyRightOriginalLeftUnequalsRight() {
        EntityDescriptor entityDescriptor = TestdataEntity.buildEntityDescriptor();

        EntitySelector leftEntitySelector = SelectorTestUtils.mockEntitySelector(entityDescriptor,
                new TestdataEntity("a"), new TestdataEntity("b"), new TestdataEntity("c"), new TestdataEntity("d"));

        EntitySelector rightEntitySelector = SelectorTestUtils.mockEntitySelector(entityDescriptor);

        SwapMoveSelector moveSelector = new SwapMoveSelector(leftEntitySelector, rightEntitySelector,
                leftEntitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), false);

        SolverScope solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        assertAllCodesOfMoveSelector(moveSelector);
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(leftEntitySelector, 1, 2, 5);
        verifyPhaseLifecycle(rightEntitySelector, 1, 2, 5);
    }

    @Test
    void originalEntitiesPinned() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var e1 = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v4));
        var e2 = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        var e3 = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v1, v4));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var leftEntitySelector = new ManualEntityMimicRecorder<>(
                mockEntitySelector(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor(), e1, e2, e3));

        var replayingEntitySelector = new MimicReplayingEntitySelector<>(leftEntitySelector);
        var filteringEntitySelector =
                FilteringEntitySelector.of(
                        mockEntitySelector(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor(), e1, e2, e3),
                        new EntityCodeFiltering<>(List.of("B", "C")));
        var rightEntitySelector =
                new FilteringEntityByEntitySelector<>(filteringEntitySelector, replayingEntitySelector, false);
        var solverScope = solvingStarted(rightEntitySelector, scoreDirector);
        phaseStarted(rightEntitySelector, solverScope);

        // Regular iterator
        // The left selector chooses A, and the right selector returns no value
        leftEntitySelector.setRecordedEntity(e1);
        var iterator = rightEntitySelector.iterator();
        assertThat(iterator.hasNext()).isFalse();
        // The left selector chooses B, and the right selector returns A
        leftEntitySelector.setRecordedEntity(e2);
        iterator = rightEntitySelector.iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).hasToString("A");
        // No more moves
        assertThat(iterator.hasNext()).isFalse();

        leftEntitySelector.setRecordedEntity(e1);
        iterator = rightEntitySelector.endingIterator();
        assertThat(iterator.hasNext()).isFalse();
        // The left selector chooses B, and the right selector returns A
        leftEntitySelector.setRecordedEntity(e2);
        iterator = rightEntitySelector.iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).hasToString("A");
        // No more moves
        assertThat(iterator.hasNext()).isFalse();

        // ListIterator
        // The left selector chooses A, and the right selector returns no value
        leftEntitySelector.setRecordedEntity(e1);
        var listIterator = rightEntitySelector.listIterator();
        assertThat(listIterator.hasNext()).isFalse();
        // B <-> A
        leftEntitySelector.setRecordedEntity(e2);
        listIterator = rightEntitySelector.listIterator();
        assertThat(listIterator.hasNext()).isTrue();
        assertThat(listIterator.next()).hasToString("A");
        assertThat(listIterator.hasNext()).isFalse();
        // Backward move
        assertThat(listIterator.hasPrevious()).isTrue();
        assertThat(listIterator.previous()).hasToString("A");

        leftEntitySelector.setRecordedEntity(e1);
        listIterator = rightEntitySelector.listIterator(0);
        assertThat(listIterator.hasNext()).isFalse();
        // B <-> A
        leftEntitySelector.setRecordedEntity(e2);
        listIterator = rightEntitySelector.listIterator();
        assertThat(listIterator.hasNext()).isTrue();
        assertThat(listIterator.next()).hasToString("A");
        assertThat(listIterator.hasNext()).isFalse();
        // Backward move
        assertThat(listIterator.hasPrevious()).isTrue();
        assertThat(listIterator.previous()).hasToString("A");
    }

    @Test
    void randomEntitiesPinned() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var e1 = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v4), v1);
        var e2 = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3), v2);
        var e3 = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v1, v4));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var baseEntitySelector =
                new FromSolutionEntitySelector<>(getEntityDescriptor(scoreDirector), SelectionCacheType.JUST_IN_TIME, true);
        var leftEntitySelector = new ManualEntityMimicRecorder<>(baseEntitySelector);

        var replayingEntitySelector = new MimicReplayingEntitySelector<>(leftEntitySelector);
        var filteringEntitySelector =
                FilteringEntitySelector.of(baseEntitySelector, new EntityCodeFiltering<>(List.of("B", "C")));
        var rightEntitySelector =
                new FilteringEntityByEntitySelector<>(filteringEntitySelector, replayingEntitySelector, true);
        var solverScope = solvingStarted(rightEntitySelector, scoreDirector,
                new TestRandom(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1));
        phaseStarted(rightEntitySelector, solverScope);

        // Random iterator
        // The left selector chooses B,
        // and the right selector chooses A (not reachable), and then only B (excluded by the filter)
        leftEntitySelector.setRecordedEntity(e2);
        var iterator = rightEntitySelector.iterator();
        assertThat(iterator.hasNext()).isTrue();
        // Return the same as the left selector
        assertThat(iterator.next()).hasToString("B");
    }

    @Test
    void singleVarRandomSelectionWithEntityValueRange() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var e1 = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v4));
        var e2 = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        var e3 = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v1, v4));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var leftEntitySelector =
                new FromSolutionEntitySelector<>(getEntityDescriptor(scoreDirector), SelectionCacheType.JUST_IN_TIME, true);
        var entityMimicRecorder = new MimicRecordingEntitySelector<>(leftEntitySelector);

        var replayingEntitySelector = new MimicReplayingEntitySelector<>(entityMimicRecorder);
        var rightEntitySelector =
                new FilteringEntityByEntitySelector<>(leftEntitySelector, replayingEntitySelector, true);

        var moveSelector = new SwapMoveSelector<>(entityMimicRecorder, rightEntitySelector,
                leftEntitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), true);

        var random = new TestRandom(0);
        var solverScope = solvingStarted(moveSelector, scoreDirector, random);
        phaseStarted(moveSelector, solverScope);
        var expectedSize = (long) solution.getEntityList().size() * solution.getEntityList().size();

        // e1(null) and e2(null)
        // only e3 is reachable by e1
        scoreDirector.setWorkingSolution(solution);
        // select left A, select right C
        // select left A, select right B
        random.reset(0, 2, 0, 1, 0, 2);
        assertCodesOfNeverEndingIterableSelector(moveSelector, expectedSize, "A<->C");

        // e1(v1), e2(v3) and e3(v4)
        // e1 does not accepts v3 and e2 does not accepts v1
        // e1 accepts v4, and e3 accepts v1
        e1.setValue(v1);
        e2.setValue(v3);
        e3.setValue(v4);
        // select left A, select right C
        random.reset(0, 1, 0, 0, 0, 0);
        scoreDirector.setWorkingSolution(solution);
        assertCodesOfNeverEndingIterableSelector(moveSelector, expectedSize, "A<->C");
    }

    @Test
    void multiVarRandomSelectionWithEntityValueRange() {
        var solution = new TestdataAllowsUnassignedMultiVarEntityProvidingSolution();
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var e1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("A", List.of(v1, v4), List.of(v1, v4));
        var e2 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("B", List.of(v2, v3), List.of(v2, v3));
        var e3 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("C", List.of(v1, v4), List.of(v1, v3, v4));
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector =
                mockScoreDirector(TestdataAllowsUnassignedMultiVarEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var leftEntitySelector =
                new FromSolutionEntitySelector<>(getEntityDescriptor(scoreDirector), SelectionCacheType.JUST_IN_TIME, true);
        var entityMimicRecorder = new MimicRecordingEntitySelector<>(leftEntitySelector);

        var replayingEntitySelector = new MimicReplayingEntitySelector<>(entityMimicRecorder);
        var rightEntitySelector =
                new FilteringEntityByEntitySelector<>(leftEntitySelector, replayingEntitySelector, true);

        var moveSelector = new SwapMoveSelector<>(entityMimicRecorder, rightEntitySelector,
                leftEntitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), true);

        var random = new TestRandom(0);
        var solverScope = solvingStarted(moveSelector, scoreDirector, random);
        phaseStarted(moveSelector, solverScope);
        var expectedSize = (long) solution.getEntityList().size() * solution.getEntityList().size();

        // e1(null, null) and e2(null, null)
        // we assume that any entity is reachable if they share at least one common value in the range
        scoreDirector.setWorkingSolution(solution);
        // select left A, select right B
        // select left A, select right C
        random.reset(0, 1, 0, 2, 0, 2);
        assertCodesOfNeverEndingIterableSelector(moveSelector, expectedSize, "A<->B", "A<->C");

        // e1(v1, v1), e2(v3, v3) and e3(v4, v4)
        // e1 does not accepts v3 and e2 does not accepts v1
        // e1 accepts v4, and e3 accepts v1
        e1.setValue(v1);
        e1.setSecondValue(v1);
        e2.setValue(v3);
        e2.setSecondValue(v3);
        e3.setValue(v4);
        e3.setSecondValue(v4);
        // select left A, select right C
        // select left A, select right C
        random.reset(0, 2, 0, 2);
        scoreDirector.setWorkingSolution(solution);
        assertCodesOfNeverEndingIterableSelector(moveSelector, expectedSize, "A<->C");

        // e1(v1, v1), e2(v3, v3) and e3(v3, v4)
        // e1 accepts v4 in the first variable, but it does not accept v3 in the second variable
        e1.setValue(v1);
        e1.setSecondValue(v1);
        e2.setValue(v3);
        e2.setSecondValue(v3);
        e3.setValue(v4);
        e3.setSecondValue(v3);
        // select left A, select right C
        random.reset(0, 2, 0, 2);
        scoreDirector.setWorkingSolution(solution);
        assertCodesOfNeverEndingIterableSelector(moveSelector, DO_NOT_ASSERT_SIZE);
    }

    @Test
    void noReachableEntities() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        // Each entity has a different value, which makes impossible to do swaps
        var e1 = new TestdataEntityProvidingEntity("A", List.of(v1), v1);
        var e2 = new TestdataEntityProvidingEntity("B", List.of(v2), v2);
        var e3 = new TestdataEntityProvidingEntity("C", List.of(v3), v3);
        var solution = new TestdataEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var leftEntitySelector =
                new FromSolutionEntitySelector<>(getEntityDescriptor(scoreDirector), SelectionCacheType.JUST_IN_TIME, true);
        var entityMimicRecorder = new MimicRecordingEntitySelector<>(leftEntitySelector);

        var replayingEntitySelector = new MimicReplayingEntitySelector<>(entityMimicRecorder);
        var rightEntitySelector =
                new FilteringEntityByEntitySelector<>(leftEntitySelector, replayingEntitySelector, true);

        var moveSelector = new SwapMoveSelector<>(entityMimicRecorder, rightEntitySelector,
                leftEntitySelector.getEntityDescriptor().getGenuineVariableDescriptorList(), true);

        var solverScope = solvingStarted(moveSelector, scoreDirector, new Random(0));
        phaseStarted(moveSelector, solverScope);
        scoreDirector.setWorkingSolution(solution);

        // The iterator is not able to find a reachable entity, but the random iterator will return has next as true
        var iterator = moveSelector.iterator();
        assertThat(iterator.hasNext()).isTrue();
        var swapMove = (SelectorBasedSwapMove<TestdataEntityProvidingSolution>) iterator.next();
        assertThat(swapMove.getLeftEntity()).isSameAs(swapMove.getRightEntity());
    }

    private static class EntityCodeFiltering<Solution_> implements SelectionFilter<Solution_, Object> {

        private final List<String> excludedCodes;

        public EntityCodeFiltering(List<String> excludedCodes) {
            this.excludedCodes = excludedCodes;
        }

        @Override
        public boolean accept(ScoreDirector<Solution_> scoreDirector, Object selection) {
            return !excludedCodes.contains(((TestdataObject) selection).getCode());
        }
    }
}
