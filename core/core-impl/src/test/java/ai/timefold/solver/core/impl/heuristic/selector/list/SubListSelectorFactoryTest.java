package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.MimicRecordingSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.MimicReplayingSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.SubListMimicRecorder;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils;

import org.junit.jupiter.api.Test;

class SubListSelectorFactoryTest {

    @Test
    void buildSubListSelector() {
        SubListSelectorConfig config = new SubListSelectorConfig()
                .withMinimumSubListSize(2)
                .withMaximumSubListSize(3)
                .withValueSelectorConfig(new ValueSelectorConfig());

        SubListSelectorFactory<TestdataListSolution> selectorFactory = SubListSelectorFactory.create(config);

        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();
        EntitySelector<TestdataListSolution> entitySelector = TestdataListUtils.mockEntitySelector(new TestdataListEntity[0]);
        when(entitySelector.getEntityDescriptor()).thenReturn(listVariableDescriptor.getEntityDescriptor());

        RandomSubListSelector<TestdataListSolution> subListSelector =
                (RandomSubListSelector<TestdataListSolution>) selectorFactory.buildSubListSelector(heuristicConfigPolicy,
                        entitySelector, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);

        assertThat(subListSelector.getMinimumSubListSize()).isEqualTo(config.getMinimumSubListSize());
        assertThat(subListSelector.getMaximumSubListSize()).isEqualTo(config.getMaximumSubListSize());
    }

    @Test
    void buildMimicRecordingSelector() {
        SubListSelectorConfig config = new SubListSelectorConfig()
                .withId("someSelectorId")
                .withMinimumSubListSize(3)
                .withMaximumSubListSize(10)
                .withValueSelectorConfig(new ValueSelectorConfig());

        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();
        EntitySelector<TestdataListSolution> entitySelector = TestdataListUtils.mockEntitySelector(new TestdataListEntity[0]);
        when(entitySelector.getEntityDescriptor()).thenReturn(listVariableDescriptor.getEntityDescriptor());

        SubListSelectorFactory<TestdataListSolution> selectorFactory = SubListSelectorFactory.create(config);
        SubListSelector<TestdataListSolution> subListSelector = selectorFactory.buildSubListSelector(heuristicConfigPolicy,
                entitySelector, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);

        assertThat(subListSelector).isInstanceOf(MimicRecordingSubListSelector.class);
    }

    @Test
    void buildMimicReplayingSelector() {
        String selectorId = "someSelectorId";
        SubListSelectorConfig config = new SubListSelectorConfig()
                .withMimicSelectorRef(selectorId);

        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();
        EntitySelector<TestdataListSolution> entitySelector = TestdataListUtils.mockEntitySelector(new TestdataListEntity[0]);
        when(entitySelector.getEntityDescriptor()).thenReturn(listVariableDescriptor.getEntityDescriptor());

        SubListMimicRecorder<TestdataListSolution> subListMimicRecorder = mock(SubListMimicRecorder.class);
        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());
        heuristicConfigPolicy.addSubListMimicRecorder(selectorId, subListMimicRecorder);
        when(subListMimicRecorder.getVariableDescriptor()).thenReturn(listVariableDescriptor);

        SubListSelectorFactory<TestdataListSolution> selectorFactory = SubListSelectorFactory.create(config);
        SubListSelector<TestdataListSolution> subListSelector = selectorFactory.buildSubListSelector(heuristicConfigPolicy,
                entitySelector, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);

        assertThat(subListSelector).isInstanceOf(MimicReplayingSubListSelector.class);
    }

    @Test
    void failFast_ifMimicRecordingIsUsedWithOtherProperty() {
        SubListSelectorConfig config = new SubListSelectorConfig()
                .withMaximumSubListSize(10)
                .withMimicSelectorRef("someSelectorId");

        assertThatIllegalArgumentException().isThrownBy(
                () -> SubListSelectorFactory.<TestdataListSolution> create(config)
                        .buildMimicReplaying(buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor())))
                .withMessageContaining("has another property");
    }

    @Test
    void requiresListVariable() {
        SubListSelectorConfig subListSelectorConfig = new SubListSelectorConfig();

        EntitySelector<TestdataSolution> entitySelector =
                SelectorTestUtils.mockEntitySelector(TestdataEntity.buildEntityDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> SubListSelectorFactory.<TestdataSolution> create(subListSelectorConfig)
                        .buildSubListSelector(buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor()),
                                entitySelector, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM))
                .withMessageContaining("@" + PlanningListVariable.class.getSimpleName());
    }
}
