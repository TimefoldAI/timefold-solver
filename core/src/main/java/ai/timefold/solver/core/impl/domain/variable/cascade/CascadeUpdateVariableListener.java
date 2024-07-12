package ai.timefold.solver.core.impl.domain.variable.cascade;

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

    private final ShadowVariableDescriptor<Solution_> sourceShadowVariableDescriptor;
    private final ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;
    private final MemberAccessor listenerUpdateMethod;

    public CascadeUpdateVariableListener(ShadowVariableDescriptor<Solution_> sourceShadowVariableDescriptor,
            ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor, MemberAccessor listenerUpdateMethod) {
        this.sourceShadowVariableDescriptor = sourceShadowVariableDescriptor;
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
            Object oldValue = sourceShadowVariableDescriptor.getValue(currentEntity);
            listenerUpdateMethod.executeGetter(currentEntity);
            Object newValue = sourceShadowVariableDescriptor.getValue(currentEntity);
            if (!Objects.equals(oldValue, newValue)) {
                sourceShadowVariableDescriptor.setValue(currentEntity, oldValue);
                scoreDirector.beforeVariableChanged(currentEntity, sourceShadowVariableDescriptor.getVariableName());
                sourceShadowVariableDescriptor.setValue(currentEntity, newValue);
                scoreDirector.afterVariableChanged(currentEntity, sourceShadowVariableDescriptor.getVariableName());
            } else {
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
