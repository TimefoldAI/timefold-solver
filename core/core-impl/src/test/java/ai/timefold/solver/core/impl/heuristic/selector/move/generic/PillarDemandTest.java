package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.SubPillarConfigPolicy;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.FromSolutionEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntitySelector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PillarDemandTest {

    @Test
    void equality() {
        SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        EntityDescriptor<TestdataSolution> entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataEntity.class);
        List<GenuineVariableDescriptor<TestdataSolution>> variableDescriptorList =
                entityDescriptor.getGenuineVariableDescriptorList();
        SubPillarConfigPolicy subPillarConfigPolicy = SubPillarConfigPolicy.withoutSubpillars();

        EntitySelector<TestdataSolution> entitySelector =
                new FromSolutionEntitySelector<>(entityDescriptor, SelectionCacheType.JUST_IN_TIME, true);
        SelectionFilter<TestdataSolution, Object> selectionFilter = (scoreDirector, selection) -> true;
        FilteringEntitySelector<TestdataSolution> filteringEntitySelector =
                FilteringEntitySelector.of(entitySelector, selectionFilter);

        PillarDemand<TestdataSolution> pillarDemand =
                new PillarDemand<>(filteringEntitySelector, variableDescriptorList, subPillarConfigPolicy);
        Assertions.assertThat(pillarDemand).isEqualTo(pillarDemand);

        PillarDemand<TestdataSolution> samePillarDemand =
                new PillarDemand<>(filteringEntitySelector, variableDescriptorList, subPillarConfigPolicy);
        Assertions.assertThat(samePillarDemand).isEqualTo(pillarDemand);

        PillarDemand<TestdataSolution> samePillarDemandCopiedList =
                new PillarDemand<>(filteringEntitySelector, new ArrayList<>(variableDescriptorList), subPillarConfigPolicy);
        Assertions.assertThat(samePillarDemandCopiedList).isEqualTo(pillarDemand);

        EntitySelector<TestdataSolution> sameEntitySelector = FilteringEntitySelector.of(entitySelector, selectionFilter);
        PillarDemand<TestdataSolution> samePillarDemandCopiedSelector =
                new PillarDemand<>(sameEntitySelector, new ArrayList<>(variableDescriptorList), subPillarConfigPolicy);
        Assertions.assertThat(samePillarDemandCopiedSelector).isEqualTo(pillarDemand);
    }

}
