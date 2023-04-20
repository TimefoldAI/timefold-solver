package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.impl.constructionheuristic.placer.entity.PlacementAssertions.assertValuePlacement;
import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacer;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacerFactory;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.Test;

class QueuedValuePlacerFactoryTest {

    @Test
    void buildEntityPlacer_withoutConfiguredMoveSelector() {
        QueuedValuePlacerConfig config = new QueuedValuePlacerConfig()
                .withEntityClass(TestdataEntity.class);

        QueuedValuePlacer<TestdataSolution> placer =
                new QueuedValuePlacerFactory<TestdataSolution>(config)
                        .buildEntityPlacer(buildHeuristicConfigPolicy());

        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        when(phaseScope.getScoreDirector()).thenReturn((InnerScoreDirector) scoreDirector);
        when(scoreDirector.getWorkingSolution()).thenReturn(generateSolution());
        placer.phaseStarted(phaseScope);
        Iterator<Placement<TestdataSolution>> placementIterator = placer.iterator();
        assertThat(placementIterator).hasNext();

        AbstractStepScope<TestdataSolution> stepScope = mock(AbstractStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(stepScope.getScoreDirector()).thenReturn((InnerScoreDirector) scoreDirector);
        placer.stepStarted(stepScope);
        Placement<TestdataSolution> placement = placementIterator.next();

        assertValuePlacement(placement, "v1", "e1", "e2");
    }

    private TestdataSolution generateSolution() {
        TestdataEntity entity1 = new TestdataEntity("e1");
        TestdataEntity entity2 = new TestdataEntity("e2");
        TestdataValue value1 = new TestdataValue("v1");
        TestdataValue value2 = new TestdataValue("v2");
        TestdataSolution solution = new TestdataSolution();
        solution.setEntityList(Arrays.asList(entity1, entity2));
        solution.setValueList(Arrays.asList(value1, value2));
        return solution;
    }
}
