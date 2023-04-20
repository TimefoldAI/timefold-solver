package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.impl.constructionheuristic.placer.entity.PlacementAssertions.assertEntityPlacement;
import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedEntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedEntityPlacerFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarSolution;

import org.junit.jupiter.api.Test;

class QueuedEntityPlacerFactoryTest {

    @Test
    void buildFromUnfoldNew() {
        SolutionDescriptor<TestdataMultiVarSolution> solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();

        ChangeMoveSelectorConfig primaryMoveSelectorConfig = new ChangeMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("primaryValue"));
        ChangeMoveSelectorConfig secondaryMoveSelectorConfig = new ChangeMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("secondaryValue"));

        HeuristicConfigPolicy<TestdataMultiVarSolution> configPolicy = buildHeuristicConfigPolicy(solutionDescriptor);
        QueuedEntityPlacerConfig placerConfig = QueuedEntityPlacerFactory.unfoldNew(configPolicy,
                Arrays.asList(primaryMoveSelectorConfig, secondaryMoveSelectorConfig));

        assertThat(placerConfig.getEntitySelectorConfig().getEntityClass()).isAssignableFrom(TestdataMultiVarEntity.class);
        assertThat(placerConfig.getMoveSelectorConfigList())
                .hasSize(2)
                .hasOnlyElementsOfType(ChangeMoveSelectorConfig.class);

        QueuedEntityPlacer<TestdataMultiVarSolution> entityPlacer =
                new QueuedEntityPlacerFactory<TestdataMultiVarSolution>(placerConfig)
                        .buildEntityPlacer(configPolicy);

        SolverScope<TestdataMultiVarSolution> solverScope = mock(SolverScope.class);
        entityPlacer.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataMultiVarSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        InnerScoreDirector<TestdataMultiVarSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        when(phaseScope.getScoreDirector()).thenReturn((InnerScoreDirector) scoreDirector);
        when(scoreDirector.getWorkingSolution()).thenReturn(generateTestdataSolution());
        entityPlacer.phaseStarted(phaseScope);
        Iterator<Placement<TestdataMultiVarSolution>> placementIterator = entityPlacer.iterator();
        assertThat(placementIterator).hasNext();

        AbstractStepScope<TestdataMultiVarSolution> stepScope = mock(AbstractStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(stepScope.getScoreDirector()).thenReturn((InnerScoreDirector) scoreDirector);
        entityPlacer.stepStarted(stepScope);
        Placement<TestdataMultiVarSolution> placement = placementIterator.next();

        assertEntityPlacement(placement, "e1", "e1v1", "e1v2", "e2v1", "e2v2");
    }

    private TestdataMultiVarSolution generateTestdataSolution() {
        TestdataMultiVarEntity entity1 = new TestdataMultiVarEntity("e1");
        entity1.setPrimaryValue(new TestdataValue("e1v1"));
        entity1.setSecondaryValue(new TestdataValue("e1v2"));
        TestdataMultiVarEntity entity2 = new TestdataMultiVarEntity("e2");
        entity2.setPrimaryValue(new TestdataValue("e2v1"));
        entity2.setSecondaryValue(new TestdataValue("e2v2"));

        TestdataMultiVarSolution solution = new TestdataMultiVarSolution("s");
        solution.setMultiVarEntityList(Arrays.asList(entity1, entity2));
        solution.setValueList(Arrays.asList(entity1.getPrimaryValue(), entity1.getSecondaryValue(), entity2.getPrimaryValue(),
                entity2.getSecondaryValue()));
        return solution;
    }
}
