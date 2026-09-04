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

}
