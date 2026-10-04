package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.shadow.counting.TestdataCountingEntity;
import ai.timefold.solver.core.testdomain.shadow.counting.TestdataCountingSolution;
import ai.timefold.solver.core.testdomain.shadow.counting.TestdataCountingValue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SingleDirectionalParentVariableReferenceGraphTest {

    @Test
    void supplierMethodsAreOnlyCalledOnce() {
        var solutionDescriptor = TestdataCountingSolution.buildSolutionDescriptor();
        var entity1 = new TestdataCountingEntity("e1");
        var entity2 = new TestdataCountingEntity("e2");

        var value1 = new TestdataCountingValue("v1");
        var value2 = new TestdataCountingValue("v2");
        var value3 = new TestdataCountingValue("v3");
        var value4 = new TestdataCountingValue("v4");
        var value5 = new TestdataCountingValue("v5");

        var graphStructureAndDirection = GraphStructure.determineGraphStructure(solutionDescriptor,
                entity1, entity2, value1, value2, value3, value4, value5);
        assertThat(graphStructureAndDirection.structure()).isEqualTo(GraphStructure.SINGLE_DIRECTIONAL_PARENT);

        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        var listVariableState = Mockito.mock(ListVariableState.class);
        Mockito.when(scoreDirector.getListVariableState(Mockito.any()))
                .thenReturn(listVariableState);

        value1.setEntity(entity1);
        value1.setPrevious(null);
        Mockito.doReturn(0).when(listVariableState).getIndexOrElse(Mockito.eq(value1), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value1)).thenReturn(null);
        Mockito.when(listVariableState.getInverseSingleton(value1)).thenReturn(entity1);

        value2.setEntity(entity2);
        value2.setPrevious(null);
        Mockito.doReturn(0).when(listVariableState).getIndexOrElse(Mockito.eq(value2), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value2)).thenReturn(value3);
        Mockito.when(listVariableState.getInverseSingleton(value2)).thenReturn(entity2);

        value3.setEntity(entity2);
        value3.setPrevious(value2);
        Mockito.doReturn(1).when(listVariableState).getIndexOrElse(Mockito.eq(value3), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value3)).thenReturn(value4);
        Mockito.when(listVariableState.getInverseSingleton(value3)).thenReturn(entity2);

        value4.setEntity(entity2);
        value4.setPrevious(value3);
        Mockito.doReturn(2).when(listVariableState).getIndexOrElse(Mockito.eq(value4), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value4)).thenReturn(null);
        Mockito.when(listVariableState.getInverseSingleton(value4)).thenReturn(entity2);

        value5.setEntity(null);
        value5.setPrevious(null);
        Mockito.doReturn(-1).when(listVariableState).getIndexOrElse(Mockito.eq(value5), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value5)).thenReturn(null);
        Mockito.when(listVariableState.getInverseSingleton(value5)).thenReturn(null);

        var values = List.of(value1, value2, value3, value4, value5);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        var graph = DefaultShadowVariableSessionFactory.buildSingleDirectionalParentGraph(
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(
                        solutionDescriptor, ChangedVariableNotifier.of(scoreDirector),
                        entity1, entity2, value5, value4, value3, value2, value1),
                graphStructureAndDirection);

        assertThat(value1.getCount()).isZero();
        assertThat(value2.getCount()).isZero();
        assertThat(value3.getCount()).isOne();
        assertThat(value4.getCount()).isEqualTo(2);
        assertThat(value5.getCount()).isNull();

        values.forEach(TestdataCountingValue::reset);
        Mockito.reset(listVariableState);

        value2.setPrevious(value3);
        value3.setPrevious(value5);
        value5.setEntity(entity2);
        value4.setPrevious(value2);

        Mockito.doReturn(0).when(listVariableState).getIndexOrElse(Mockito.eq(value1), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value1)).thenReturn(null);
        Mockito.when(listVariableState.getInverseSingleton(value1)).thenReturn(entity1);

        Mockito.doReturn(0).when(listVariableState).getIndexOrElse(Mockito.eq(value5), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value5)).thenReturn(value3);
        Mockito.when(listVariableState.getInverseSingleton(value5)).thenReturn(entity2);

        Mockito.doReturn(1).when(listVariableState).getIndexOrElse(Mockito.eq(value3), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value3)).thenReturn(value2);
        Mockito.when(listVariableState.getInverseSingleton(value3)).thenReturn(entity2);

        Mockito.doReturn(2).when(listVariableState).getIndexOrElse(Mockito.eq(value2), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value2)).thenReturn(value4);
        Mockito.when(listVariableState.getInverseSingleton(value2)).thenReturn(entity2);

        Mockito.doReturn(3).when(listVariableState).getIndexOrElse(Mockito.eq(value4), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value4)).thenReturn(null);
        Mockito.when(listVariableState.getInverseSingleton(value4)).thenReturn(entity2);

        var previousVariableMetamodel =
                solutionDescriptor.getMetaModel().entity(TestdataCountingValue.class).variable("previous");
        var entityVariableMetamodel = solutionDescriptor.getMetaModel().entity(TestdataCountingValue.class).variable("entity");

        graph.afterVariableChanged(previousVariableMetamodel, value2);
        graph.afterVariableChanged(previousVariableMetamodel, value3);
        graph.afterVariableChanged(entityVariableMetamodel, value5);
        graph.afterVariableChanged(previousVariableMetamodel, value4);

        assertThatCode(graph::updateChanged).doesNotThrowAnyException();

        assertThat(value1.getCount()).isZero();
        assertThat(value2.getCount()).isEqualTo(2);
        assertThat(value3.getCount()).isOne();
        assertThat(value4.getCount()).isEqualTo(3);
        assertThat(value5.getCount()).isZero();
    }

    @Test
    void elementsUnassignedInTheSamePassAreAllUpdated() {
        var solutionDescriptor = TestdataCountingSolution.buildSolutionDescriptor();
        var entity = new TestdataCountingEntity("e1");

        var value1 = new TestdataCountingValue("v1");
        var value2 = new TestdataCountingValue("v2");
        var value3 = new TestdataCountingValue("v3");
        var value4 = new TestdataCountingValue("v4");

        var graphStructureAndDirection = GraphStructure.determineGraphStructure(solutionDescriptor,
                entity, value1, value2, value3, value4);
        assertThat(graphStructureAndDirection.structure()).isEqualTo(GraphStructure.SINGLE_DIRECTIONAL_PARENT);

        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        var listVariableState = Mockito.mock(ListVariableState.class);
        Mockito.when(scoreDirector.getListVariableState(Mockito.any()))
                .thenReturn(listVariableState);

        // The entity's list variable is [value1, value2, value3, value4].
        value1.setEntity(entity);
        value1.setPrevious(null);
        Mockito.doReturn(0).when(listVariableState).getIndexOrElse(Mockito.eq(value1), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value1)).thenReturn(value2);
        Mockito.when(listVariableState.getInverseSingleton(value1)).thenReturn(entity);

        value2.setEntity(entity);
        value2.setPrevious(value1);
        Mockito.doReturn(1).when(listVariableState).getIndexOrElse(Mockito.eq(value2), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value2)).thenReturn(value3);
        Mockito.when(listVariableState.getInverseSingleton(value2)).thenReturn(entity);

        value3.setEntity(entity);
        value3.setPrevious(value2);
        Mockito.doReturn(2).when(listVariableState).getIndexOrElse(Mockito.eq(value3), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value3)).thenReturn(value4);
        Mockito.when(listVariableState.getInverseSingleton(value3)).thenReturn(entity);

        value4.setEntity(entity);
        value4.setPrevious(value3);
        Mockito.doReturn(3).when(listVariableState).getIndexOrElse(Mockito.eq(value4), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value4)).thenReturn(null);
        Mockito.when(listVariableState.getInverseSingleton(value4)).thenReturn(entity);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        var graph = DefaultShadowVariableSessionFactory.buildSingleDirectionalParentGraph(
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(
                        solutionDescriptor, ChangedVariableNotifier.of(scoreDirector),
                        entity, value1, value2, value3, value4),
                graphStructureAndDirection);

        assertThat(value1.getCount()).isZero();
        assertThat(value2.getCount()).isOne();
        assertThat(value3.getCount()).isEqualTo(2);
        assertThat(value4.getCount()).isEqualTo(3);

        List.of(value1, value2, value3, value4).forEach(TestdataCountingValue::reset);
        Mockito.reset(listVariableState);

        // Unassigns value2 and value3 in one move, leaving [value1, value4] - value4 now follows value1.
        value2.setEntity(null);
        value2.setPrevious(null);
        value3.setEntity(null);
        value3.setPrevious(null);
        value4.setPrevious(value1);

        Mockito.doReturn(0).when(listVariableState).getIndexOrElse(Mockito.eq(value1), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value1)).thenReturn(value4);
        Mockito.when(listVariableState.getInverseSingleton(value1)).thenReturn(entity);

        Mockito.doReturn(1).when(listVariableState).getIndexOrElse(Mockito.eq(value4), Mockito.anyInt());
        Mockito.when(listVariableState.getNextElement(value4)).thenReturn(null);
        Mockito.when(listVariableState.getInverseSingleton(value4)).thenReturn(entity);

        // An unassigned element has no index, so getIndexOrElse gives back its default,
        // no next element, and no inverse entity.
        for (var unassignedValue : List.of(value2, value3)) {
            Mockito.doReturn(0).when(listVariableState).getIndexOrElse(Mockito.eq(unassignedValue), Mockito.anyInt());
            Mockito.when(listVariableState.getNextElement(unassignedValue)).thenReturn(null);
            Mockito.when(listVariableState.getInverseSingleton(unassignedValue)).thenReturn(null);
        }

        var metaModel = solutionDescriptor.getMetaModel().entity(TestdataCountingValue.class);
        var previousVariableMetamodel = metaModel.variable("previous");
        var entityVariableMetamodel = metaModel.variable("entity");

        // A real unassign fires afterListVariableElementUnassigned, which changes both parent
        // variables; countSupplier fails if it's called twice for the same element.
        scoreDirector.afterListVariableElementUnassigned(entity, "values", value2);
        graph.afterVariableChanged(entityVariableMetamodel, value2);
        graph.afterVariableChanged(previousVariableMetamodel, value2);
        scoreDirector.afterListVariableElementUnassigned(entity, "values", value3);
        graph.afterVariableChanged(entityVariableMetamodel, value3);
        graph.afterVariableChanged(previousVariableMetamodel, value3);
        // value4's previous changed too: from value3 to value1.
        graph.afterVariableChanged(previousVariableMetamodel, value4);

        graph.updateChanged();

        // An unassigned element has no count; it is in no list for its supplier to count along.
        assertThat(value2.getCount()).isNull();
        assertThat(value3.getCount()).isNull();
        // value1 is still the first element of the list, so its count is unchanged.
        assertThat(value1.getCount()).isZero();
        // value4 now follows value1, so its count drops from the stale 3 to 1.
        assertThat(value4.getCount()).isEqualTo(1);
    }

}
