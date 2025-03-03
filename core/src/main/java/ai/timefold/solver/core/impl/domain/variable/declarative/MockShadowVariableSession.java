package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSession;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class MockShadowVariableSession<Solution_> implements ShadowVariableSession {
    final SolutionDescriptor<Solution_> solutionDescriptor;
    final VariableReferenceGraph<Solution_> graph;
    final MockListStateSupply<Solution_> mockListStateSupply;

    public MockShadowVariableSession(SolutionDescriptor<Solution_> solutionDescriptor, VariableReferenceGraph<Solution_> graph,
            MockListStateSupply<Solution_> mockListStateSupply) {
        this.solutionDescriptor = solutionDescriptor;
        this.graph = graph;
        this.mockListStateSupply = mockListStateSupply;
    }

    @Override
    public void setVariable(Object entity, String variableName, @Nullable Object value) {
        var variableId = VariableId.entity(entity.getClass()).child(variableName);
        graph.beforeVariableChanged(variableId, entity);
        solutionDescriptor.getEntityDescriptorStrict(entity.getClass()).getVariableDescriptor(variableName)
                .setValue(entity, value);
        graph.afterVariableChanged(variableId, entity);
    }

    @Override
    public void setPrevious(Object entity, @Nullable Object previousValue) {
        var variableId = VariableId.entity(entity.getClass()).child(DefaultShadowVariableFactory.PREVIOUS);
        var inverseVariableId =
                VariableId.entity(entity.getClass()).child(DefaultShadowVariableFactory.NEXT);
        var oldPrevious = mockListStateSupply.getPreviousElement(entity);
        graph.beforeVariableChanged(variableId, entity);
        graph.beforeVariableChanged(inverseVariableId, previousValue);
        if (oldPrevious != null) {
            graph.beforeVariableChanged(inverseVariableId, oldPrevious);
        }
        mockListStateSupply.setPrevious(entity, previousValue);
        graph.afterVariableChanged(variableId, entity);
        graph.afterVariableChanged(inverseVariableId, previousValue);
        if (oldPrevious != null) {
            graph.afterVariableChanged(inverseVariableId, oldPrevious);
        }
    }

    @Override
    public void setNext(Object entity, @Nullable Object nextValue) {
        var variableId = VariableId.entity(entity.getClass()).child(DefaultShadowVariableFactory.NEXT);
        var inverseVariableId =
                VariableId.entity(entity.getClass()).child(DefaultShadowVariableFactory.PREVIOUS);
        var oldNext = mockListStateSupply.getNextElement(entity);
        graph.beforeVariableChanged(variableId, entity);
        graph.beforeVariableChanged(inverseVariableId, nextValue);
        if (oldNext != null) {
            graph.beforeVariableChanged(inverseVariableId, oldNext);
        }
        mockListStateSupply.setNext(entity, nextValue);
        graph.afterVariableChanged(variableId, entity);
        graph.afterVariableChanged(inverseVariableId, nextValue);
        if (oldNext != null) {
            graph.afterVariableChanged(inverseVariableId, oldNext);
        }
    }

    @Override
    public void setInverse(Object entity, @Nullable Object inverseValue) {
        var variableId = VariableId.entity(entity.getClass()).child(DefaultShadowVariableFactory.INVERSE);
        graph.beforeVariableChanged(variableId, entity);
        mockListStateSupply.setInverse(entity, inverseValue);
        graph.afterVariableChanged(variableId, entity);
    }

    @Override
    public void updateVariables() {
        graph.updateChanged();
    }
}
