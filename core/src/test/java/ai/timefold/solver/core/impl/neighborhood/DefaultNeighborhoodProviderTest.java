package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhood;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.AssignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListAssignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListSwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListTailSwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListUnassignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.SwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.TwoOptListMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.UnassignMoveProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class DefaultNeighborhoodProviderTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void basicVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var builder = new DefaultNeighborhoodBuilder<>(solutionMetaModel);
        var neighborhood = (DefaultNeighborhood<TestdataSolution>) new DefaultNeighborhoodProvider<TestdataSolution>()
                .defineNeighborhood(builder);
        assertThat(neighborhood.moveProviderMap().values())
                .map(c -> (Class) c.getClass())
                .containsExactly(ChangeMoveProvider.class, SwapMoveProvider.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void basicVariableAllowsUnassigned() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var builder = new DefaultNeighborhoodBuilder<>(solutionMetaModel);
        var neighborhood =
                (DefaultNeighborhood<TestdataAllowsUnassignedSolution>) new DefaultNeighborhoodProvider<TestdataAllowsUnassignedSolution>()
                        .defineNeighborhood(builder);
        assertThat(neighborhood.moveProviderMap().values())
                .map(c -> (Class) c.getClass())
                .containsExactly(ChangeMoveProvider.class, AssignMoveProvider.class,
                        UnassignMoveProvider.class, SwapMoveProvider.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void listVariable() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var builder = new DefaultNeighborhoodBuilder<>(solutionMetaModel);
        var neighborhood = (DefaultNeighborhood<TestdataListSolution>) new DefaultNeighborhoodProvider<TestdataListSolution>()
                .defineNeighborhood(builder);
        assertThat(neighborhood.moveProviderMap().values())
                .map(c -> (Class) c.getClass())
                .containsExactly(ListChangeMoveProvider.class, ListSwapMoveProvider.class,
                        TwoOptListMoveProvider.class, ListTailSwapMoveProvider.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void listVariableAllowsUnassignedValues() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var builder = new DefaultNeighborhoodBuilder<>(solutionMetaModel);
        var neighborhood =
                (DefaultNeighborhood<TestdataAllowsUnassignedValuesListSolution>) new DefaultNeighborhoodProvider<TestdataAllowsUnassignedValuesListSolution>()
                        .defineNeighborhood(builder);
        assertThat(neighborhood.moveProviderMap().values())
                .map(c -> (Class) c.getClass())
                .containsExactly(ListChangeMoveProvider.class, ListSwapMoveProvider.class,
                        TwoOptListMoveProvider.class, ListTailSwapMoveProvider.class,
                        ListAssignMoveProvider.class, ListUnassignMoveProvider.class);
    }

    @Test
    void uniqueIds() {
        assertThat(idsOf(TestdataSolution.buildMetaModel())).containsExactly(
                "ChangeMoveProvider(TestdataEntity.value,noCrossingNull)",
                "SwapMoveProvider(TestdataEntity.value)");
        assertThat(idsOf(TestdataAllowsUnassignedSolution.buildMetaModel())).containsExactly(
                "ChangeMoveProvider(TestdataAllowsUnassignedEntity.value,noCrossingNull)",
                "AssignMoveProvider(TestdataAllowsUnassignedEntity.value)",
                "UnassignMoveProvider(TestdataAllowsUnassignedEntity.value)",
                "SwapMoveProvider(TestdataAllowsUnassignedEntity.value)");
        assertThat(idsOf(TestdataListSolution.buildMetaModel())).containsExactly(
                "ListChangeMoveProvider(TestdataListEntity.valueList,noCrossingNull)",
                "ListSwapMoveProvider(TestdataListEntity.valueList)",
                "TwoOptListMoveProvider(TestdataListEntity.valueList,noCrossingEntity)",
                "ListTailSwapMoveProvider(TestdataListEntity.valueList,reversing)");
        assertThat(idsOf(TestdataAllowsUnassignedValuesListSolution.buildMetaModel())).containsExactly(
                "ListChangeMoveProvider(TestdataAllowsUnassignedValuesListEntity.valueList,noCrossingNull)",
                "ListSwapMoveProvider(TestdataAllowsUnassignedValuesListEntity.valueList)",
                "TwoOptListMoveProvider(TestdataAllowsUnassignedValuesListEntity.valueList,noCrossingEntity)",
                "ListTailSwapMoveProvider(TestdataAllowsUnassignedValuesListEntity.valueList,reversing)",
                "ListAssignMoveProvider(TestdataAllowsUnassignedValuesListEntity.valueList)",
                "ListUnassignMoveProvider(TestdataAllowsUnassignedValuesListEntity.valueList)");
    }

    @Test
    void sameEntitySimpleNameGetsFullName() {
        var solutionMetaModel = SolutionDescriptor.buildSolutionDescriptor(TwoTaskSolution.class,
                FirstHolder.Task.class, SecondHolder.Task.class).getMetaModel();
        assertThat(idsOf(solutionMetaModel)).containsExactlyInAnyOrder(
                "ChangeMoveProvider(Task.value,noCrossingNull)@" + FirstHolder.Task.class.getName(),
                "SwapMoveProvider(Task.value)@" + FirstHolder.Task.class.getName(),
                "ChangeMoveProvider(Task.value,noCrossingNull)@" + SecondHolder.Task.class.getName(),
                "SwapMoveProvider(Task.value)@" + SecondHolder.Task.class.getName());
    }

    private static <Solution_> Set<String> idsOf(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        var neighborhood = (DefaultNeighborhood<Solution_>) new DefaultNeighborhoodProvider<Solution_>()
                .defineNeighborhood(new DefaultNeighborhoodBuilder<>(solutionMetaModel));
        return neighborhood.moveProviderMap().keySet();
    }

    @PlanningSolution
    public static class TwoTaskSolution {

        @ValueRangeProvider
        @ProblemFactCollectionProperty
        public List<String> valueList = List.of("a", "b");
        @PlanningEntityCollectionProperty
        public List<FirstHolder.Task> firstTaskList = List.of();
        @PlanningEntityCollectionProperty
        public List<SecondHolder.Task> secondTaskList = List.of();
        @PlanningScore
        public SimpleScore score;

    }

    public static class FirstHolder {

        @PlanningEntity
        public static class Task {
            @PlanningVariable
            public String value;
        }

    }

    public static class SecondHolder {

        @PlanningEntity
        public static class Task {
            @PlanningVariable
            public String value;
        }

    }

}
