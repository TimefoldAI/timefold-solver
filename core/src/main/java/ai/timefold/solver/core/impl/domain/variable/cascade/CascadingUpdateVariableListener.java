package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementVariableSupply;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class CascadingUpdateVariableListener<Solution_> implements VariableListener<Solution_, Object> {

    private final List<VariableDescriptor<Solution_>> targetVariableDescriptorList;
    private final ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;
    private final NextElementVariableSupply nextElementVariableSupply;
    private final MemberAccessor targetMethod;

    public CascadingUpdateVariableListener(List<VariableDescriptor<Solution_>> targetVariableDescriptorList,
            ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor,
            NextElementVariableSupply nextElementVariableSupply, MemberAccessor targetMethod) {
        this.targetVariableDescriptorList = targetVariableDescriptorList;
        this.nextElementShadowVariableDescriptor = nextElementShadowVariableDescriptor;
        this.nextElementVariableSupply = nextElementVariableSupply;
        this.targetMethod = targetMethod;
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var currentEntity = entity;
        while (currentEntity != null) {
            if (!execute(scoreDirector, currentEntity, targetVariableDescriptorList, targetMethod)) {
                break;
            }
            currentEntity = getNextElement(currentEntity);
        }
    }

    private Object getNextElement(Object entity) {
        // The primary choice is to select the user-defined next element shadow variable.
        return nextElementShadowVariableDescriptor != null ? nextElementShadowVariableDescriptor.getValue(entity)
                : nextElementVariableSupply.getNext(entity);
    }

    private boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor listenerMethod) {
        if (targetVariableDescriptorList.size() == 1) {
            return executeListener(scoreDirector, entity, targetVariableDescriptorList.get(0), listenerMethod);
        } else {
            return executeListener(scoreDirector, entity, targetVariableDescriptorList, listenerMethod);
        }
    }

    private boolean executeListener(ScoreDirector<Solution_> scoreDirector, Object entity,
            VariableDescriptor<Solution_> targetVariableDescriptor, MemberAccessor listenerMethod) {
        var oldValue = targetVariableDescriptor.getValue(entity);
        scoreDirector.beforeVariableChanged(entity, targetVariableDescriptor.getVariableName());
        listenerMethod.executeGetter(entity);
        var newValue = targetVariableDescriptor.getValue(entity);
        scoreDirector.afterVariableChanged(entity, targetVariableDescriptor.getVariableName());
        return !Objects.equals(oldValue, newValue);
    }

    private boolean executeListener(ScoreDirector<Solution_> scoreDirector, Object entity,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor listenerMethod) {
        var oldValueList = new ArrayList<>(targetVariableDescriptorList.size());
        for (VariableDescriptor<Solution_> targetVariableDescriptor : targetVariableDescriptorList) {
            scoreDirector.beforeVariableChanged(entity, targetVariableDescriptor.getVariableName());
            oldValueList.add(targetVariableDescriptor.getValue(entity));
        }
        listenerMethod.executeGetter(entity);
        var newValueList = new ArrayList<>(targetVariableDescriptorList.size());
        var hasChange = false;
        for (int i = 0; i < targetVariableDescriptorList.size(); i++) {
            var targetVariableDescriptor = targetVariableDescriptorList.get(i);
            newValueList.add(targetVariableDescriptor.getValue(entity));
            scoreDirector.afterVariableChanged(entity, targetVariableDescriptor.getVariableName());
            if (!hasChange && !Objects.equals(oldValueList.get(i), newValueList.get(i))) {
                hasChange = true;
            }
        }
        return hasChange;
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

}
