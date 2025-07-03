package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.declarative.counting.TestdataCountingEntity;
import ai.timefold.solver.core.testdomain.declarative.counting.TestdataCountingSolution;
import ai.timefold.solver.core.testdomain.declarative.counting.TestdataCountingValue;

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
        var listStateSupply = Mockito.mock(ListVariableStateSupply.class);
        Mockito.when(scoreDirector.getListVariableStateSupply(Mockito.any()))
                .thenReturn(listStateSupply);

        value1.setEntity(entity1);
        value1.setPrevious(null);
        Mockito.when(listStateSupply.getIndex(value1)).thenReturn(0);
        Mockito.when(listStateSupply.getNextElement(value1)).thenReturn(null);
        Mockito.when(listStateSupply.getInverseSingleton(value1)).thenReturn(entity1);

        value2.setEntity(entity2);
        value2.setPrevious(null);
        Mockito.when(listStateSupply.getIndex(value2)).thenReturn(0);
        Mockito.when(listStateSupply.getNextElement(value2)).thenReturn(value3);
        Mockito.when(listStateSupply.getInverseSingleton(value2)).thenReturn(entity2);

        value3.setEntity(entity2);
        value3.setPrevious(value2);
        Mockito.when(listStateSupply.getIndex(value3)).thenReturn(1);
        Mockito.when(listStateSupply.getNextElement(value3)).thenReturn(value4);
        Mockito.when(listStateSupply.getInverseSingleton(value3)).thenReturn(entity2);

        value4.setEntity(entity2);
        value4.setPrevious(value3);
        Mockito.when(listStateSupply.getIndex(value4)).thenReturn(2);
        Mockito.when(listStateSupply.getNextElement(value4)).thenReturn(null);
        Mockito.when(listStateSupply.getInverseSingleton(value4)).thenReturn(entity2);

        value5.setEntity(null);
        value5.setPrevious(null);
        Mockito.when(listStateSupply.getIndex(value5)).thenReturn(-1);
        Mockito.when(listStateSupply.getNextElement(value5)).thenReturn(null);
        Mockito.when(listStateSupply.getInverseSingleton(value5)).thenReturn(null);

        var values = List.of(value1, value2, value3, value4, value5);

        @SuppressWarnings("unchecked")
        var graph = DefaultShadowVariableSessionFactory.buildSingleDirectionalParentGraph(solutionDescriptor,
                ChangedVariableNotifier.of(scoreDirector),
                graphStructureAndDirection,
                new Object[] { entity1, entity2, value5, value4, value3, value2, value1 });

        assertThat(value1.getCount()).isZero();
        assertThat(value2.getCount()).isZero();
        assertThat(value3.getCount()).isOne();
        assertThat(value4.getCount()).isEqualTo(2);
        assertThat(value5.getCount()).isNull();

        values.forEach(TestdataCountingValue::reset);
        Mockito.reset(listStateSupply);

        value2.setPrevious(value3);
        value3.setPrevious(value5);
        value5.setEntity(entity2);
        value4.setPrevious(value2);

        Mockito.when(listStateSupply.getIndex(value1)).thenReturn(0);
        Mockito.when(listStateSupply.getNextElement(value1)).thenReturn(null);
        Mockito.when(listStateSupply.getInverseSingleton(value1)).thenReturn(entity1);

        Mockito.when(listStateSupply.getIndex(value5)).thenReturn(0);
        Mockito.when(listStateSupply.getNextElement(value5)).thenReturn(value3);
        Mockito.when(listStateSupply.getInverseSingleton(value5)).thenReturn(entity2);

        Mockito.when(listStateSupply.getIndex(value3)).thenReturn(1);
        Mockito.when(listStateSupply.getNextElement(value3)).thenReturn(value2);
        Mockito.when(listStateSupply.getInverseSingleton(value3)).thenReturn(entity2);

        Mockito.when(listStateSupply.getIndex(value2)).thenReturn(2);
        Mockito.when(listStateSupply.getNextElement(value2)).thenReturn(value4);
        Mockito.when(listStateSupply.getInverseSingleton(value2)).thenReturn(entity2);

        Mockito.when(listStateSupply.getIndex(value4)).thenReturn(3);
        Mockito.when(listStateSupply.getNextElement(value4)).thenReturn(null);
        Mockito.when(listStateSupply.getInverseSingleton(value4)).thenReturn(entity2);

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
