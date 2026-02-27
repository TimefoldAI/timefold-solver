package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.variable.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListElementsChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
record DelegatingListVariableStateSupply<Solution_>(ListVariableStateSupply<Solution_, Object, Object> delegate,
        Function<Object, @Nullable Integer> indexFunction) implements ListVariableStateSupply<Solution_, Object, Object> {

    @Override
    public void externalize(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        delegate.externalize(shadowVariableDescriptor);
    }

    @Override
    public void externalize(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        delegate.externalize(shadowVariableDescriptor);
    }

    @Override
    public void externalize(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        delegate.externalize(shadowVariableDescriptor);
    }

    @Override
    public void externalize(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        delegate.externalize(shadowVariableDescriptor);
    }

    @Override
    public @Nullable Integer getIndex(Object planningValue) {
        return indexFunction.apply(planningValue);
    }

    @Override
    public @Nullable Object getInverseSingleton(Object planningValue) {
        return delegate.getInverseSingleton(planningValue);
    }

    @Override
    public ListVariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return delegate.getSourceVariableDescriptor();
    }

    @Override
    public boolean isAssigned(Object queryCompositeKey) {
        return delegate.isAssigned(queryCompositeKey);
    }

    @Override
    public boolean isPinned(Object queryCompositeKey) {
        return delegate.isPinned(queryCompositeKey);
    }

    @Override
    public ElementPosition getElementPosition(Object value) {
        return delegate.getElementPosition(value);
    }

    @Override
    public int getUnassignedCount() {
        return delegate.getUnassignedCount();
    }

    @Override
    public @Nullable Object getPreviousElement(Object queryCompositeKey) {
        return delegate.getPreviousElement(queryCompositeKey);
    }

    @Override
    public @Nullable Object getNextElement(Object queryCompositeKey) {
        return delegate.getNextElement(queryCompositeKey);
    }

    @Override
    public void afterListElementUnassigned(InnerScoreDirector<Solution_, ?> scoreDirector, Object unassignedElement) {
        delegate.afterListElementUnassigned(scoreDirector, unassignedElement);
    }

    @Override
    public void beforeChange(InnerScoreDirector<Solution_, ?> scoreDirector, ListElementsChangeEvent<Object> event) {
        delegate.beforeChange(scoreDirector, event);
    }

    @Override
    public void afterChange(InnerScoreDirector<Solution_, ?> scoreDirector, ListElementsChangeEvent<Object> event) {
        delegate.afterChange(scoreDirector, event);
    }
}
