package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockSolverScope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.SelectorBasedListRuinRecreateMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;

import org.junit.jupiter.api.Test;

class SelectorBasedListRuinRecreateMoveTest {

    @SuppressWarnings({ "unchecked" })
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

        var move = new SelectorBasedListRuinRecreateMove<TestdataListSolution>(mock(ListVariableDescriptor.class),
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mockSolverScope(), Arrays.asList(v1, v2),
                new LinkedHashSet<>(Set.of(e1, e2, e3)), 0L);
        var rebasedMove = move.rebase(destinationScoreDirector.getMoveDirector());

        assertSoftly(softly -> {
            softly.assertThat(rebasedMove.getPlanningEntities())
                    .containsExactlyInAnyOrder(destinationE1, destinationE2, destinationE3); // The input set is not ordered.
            softly.assertThat(rebasedMove.getPlanningValues())
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
        var move = new SelectorBasedListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mockSolverScope(), List.of(e1),
                new LinkedHashSet<>(Set.of(v1)), 0L);
        var sameMove = new SelectorBasedListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mockSolverScope(), List.of(e1),
                new LinkedHashSet<>(Set.of(v1)), 0L);
        assertThat(move).isEqualTo(sameMove);

        var differentMove = new SelectorBasedListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mockSolverScope(), List.of(e1),
                new LinkedHashSet<>(Set.of(v2)), 0L);
        assertThat(move).isNotEqualTo(differentMove);

        var anotherDifferentMove = new SelectorBasedListRuinRecreateMove<TestdataListSolution>(descriptor,
                mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mockSolverScope(), List.of(e2),
                new LinkedHashSet<>(Set.of(v1)), 0L);
        assertThat(move).isNotEqualTo(anotherDifferentMove);

        var yetAnotherDifferentMove =
                new SelectorBasedListRuinRecreateMove<TestdataListSolution>(mock(ListVariableDescriptor.class),
                        mock(RuinRecreateConstructionHeuristicPhaseBuilder.class), mockSolverScope(), List.of(e1),
                        new LinkedHashSet<>(Set.of(v1)), 0L);
        assertThat(move).isNotEqualTo(yetAnotherDifferentMove);
    }

    /**
     * Reproduces the before/after fromIndex mismatch in {@link SelectorBasedListRuinRecreateMove}
     * for a "new destination entity" (one that received newly recreated values without ever
     * having had any of its own values ruined) that also has a pinned prefix.
     * The nested construction heuristic is steered (via the constraint below) to always place
     * every ruined value onto entity B, which never had a value ruined from it and therefore is
     * not part of {@code entityToOriginalPositionMap}, while entity B still has 1 pinned element.
     */
    @SuppressWarnings("unchecked")
    @Test
    void executeAndUndoNewDestinationEntityWithPinnedPrefix() {
        var listVariableDescriptor = TestdataPinnedWithIndexListEntity.buildVariableDescriptorForValueList();
        var solutionDescriptor = listVariableDescriptor.getEntityDescriptor().getSolutionDescriptor();

        var aPin = new TestdataPinnedWithIndexListValue("aPin");
        var special1 = new TestdataPinnedWithIndexListValue("special1");
        var special2 = new TestdataPinnedWithIndexListValue("special2");
        var special3 = new TestdataPinnedWithIndexListValue("special3");
        var bPin = new TestdataPinnedWithIndexListValue("bPin");

        var entityA = new TestdataPinnedWithIndexListEntity("A", aPin, special1, special2, special3);
        entityA.setPinIndex(1); // aPin is pinned; special1..3 are ruined below.
        var entityB = new TestdataPinnedWithIndexListEntity("B", bPin);
        entityB.setPinIndex(1); // bPin is pinned; B never has any of its own values ruined.

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(new ArrayList<>(List.of(entityA, entityB)));
        solution.setValueList(new ArrayList<>(List.of(aPin, special1, special2, special3, bPin)));
        SolutionManager.updateShadowVariables(solution);

        // Heavily penalize any "special" value that does not end up on entity B, so the nested
        // construction heuristic is forced to recreate all 3 ruined values onto entity B.
        var scoreDirectorFactory = new EasyScoreDirectorFactory<TestdataPinnedWithIndexListSolution, SimpleScore>(
                solutionDescriptor,
                s -> {
                    var penalty = 0;
                    for (var value : s.getValueList()) {
                        if (value.getCode().startsWith("special") && value.getEntity() != null
                                && !value.getEntity().getCode().equals("B")) {
                            penalty++;
                        }
                    }
                    return SimpleScore.of(-penalty);
                }, EnvironmentMode.PHASE_ASSERT);
        var scoreDirector = (InnerScoreDirector<TestdataPinnedWithIndexListSolution, SimpleScore>) scoreDirectorFactory
                .buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);

        var solverConfigPolicy = new HeuristicConfigPolicy.Builder<TestdataPinnedWithIndexListSolution>()
                .withSolutionDescriptor(solutionDescriptor)
                .withInitializingScoreTrend(InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ANY, 1))
                .build();
        var entityPlacerConfig = DefaultConstructionHeuristicPhaseFactory
                .buildListVariableQueuedValuePlacerConfig(solverConfigPolicy, listVariableDescriptor);
        var constructionHeuristicPhaseConfig =
                new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(entityPlacerConfig);
        var constructionHeuristicPhaseBuilder =
                RuinRecreateConstructionHeuristicPhaseBuilder.create(solverConfigPolicy, constructionHeuristicPhaseConfig);

        var solverScope = new SolverScope<TestdataPinnedWithIndexListSolution>();
        solverScope.setScoreDirector(scoreDirector);

        var move = new SelectorBasedListRuinRecreateMove<TestdataPinnedWithIndexListSolution>(listVariableDescriptor,
                constructionHeuristicPhaseBuilder, solverScope, List.of(special1, special2, special3),
                new LinkedHashSet<>(Set.of(entityA)), 0L);

        // Execute the move and immediately undo it, exactly like local search does to evaluate a candidate move.
        scoreDirector.getMoveDirector().executeTemporary(move);

        assertThat(entityA.getValueList()).containsExactly(aPin, special1, special2, special3);
        assertThat(entityB.getValueList()).containsExactly(bPin);
    }

}
