package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class CascadeUpdateVariableListener<Solution_> implements VariableListener<Solution_, Object> {

    private final List<ShadowVariableDescriptor<Solution_>> sourceShadowVariableDescriptorList;
    private final ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;
    private final MemberAccessor listenerUpdateMethod;

    public CascadeUpdateVariableListener(List<ShadowVariableDescriptor<Solution_>> sourceShadowVariableDescriptorList,
            ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor, MemberAccessor listenerUpdateMethod) {
        this.sourceShadowVariableDescriptorList = sourceShadowVariableDescriptorList;
        this.nextElementShadowVariableDescriptor = nextElementShadowVariableDescriptor;
        this.listenerUpdateMethod = listenerUpdateMethod;
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var currentEntity = entity;
        while (currentEntity != null) {
            List<Object> oldValueList = new ArrayList<>(sourceShadowVariableDescriptorList.size());
            for (ShadowVariableDescriptor<Solution_> sourceShadowVariableDescriptor : sourceShadowVariableDescriptorList) {
                scoreDirector.beforeVariableChanged(currentEntity, sourceShadowVariableDescriptor.getVariableName());
                oldValueList.add(sourceShadowVariableDescriptor.getValue(currentEntity));
            }
            listenerUpdateMethod.executeGetter(currentEntity);
            List<Object> newValueList = new ArrayList<>(sourceShadowVariableDescriptorList.size());
            for (ShadowVariableDescriptor<Solution_> sourceShadowVariableDescriptor : sourceShadowVariableDescriptorList) {
                scoreDirector.afterVariableChanged(currentEntity, sourceShadowVariableDescriptor.getVariableName());
                newValueList.add(sourceShadowVariableDescriptor.getValue(currentEntity));
            }
            boolean hasChange = false;
            for (int i = 0; i < oldValueList.size(); i++) {
                if (!Objects.equals(oldValueList.get(i), newValueList.get(i))) {
                    hasChange = true;
                    break;
                }
            }
            if (!hasChange) {
                break;
            }
            currentEntity = nextElementShadowVariableDescriptor.getValue(currentEntity);
        }
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
