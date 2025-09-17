package ai.timefold.solver.core.impl.domain.variable.custom;

import java.util.Arrays;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ChangeEventType;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LegacyCustomShadowVariableBasicVariableListener<Solution_>(
        Class<?>[] sourceEntityClasses,
        VariableListener<Solution_, Object> customVariableListener)
        implements
            InnerVariableListener<Solution_, BasicVariableChangeEvent<Object>> {
    @Override
    public ChangeEventType listenedEventType() {
        return ChangeEventType.BASIC;
    }

    @Override
    public void beforeChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        customVariableListener.beforeVariableChanged(scoreDirector, event.entity());
    }

    @Override
    public void afterChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        customVariableListener.afterVariableChanged(scoreDirector, event.entity());
    }

    @Override
    public boolean requiresUniqueEntityEvents() {
        return customVariableListener.requiresUniqueEntityEvents();
    }

    @Override
    public void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        customVariableListener.resetWorkingSolution(scoreDirector);
        Arrays.stream(sourceEntityClasses).distinct()
                .forEach(sourceEntityClass -> InnerVariableListener.forEachEntity(scoreDirector,
                        sourceEntityClass,
                        entity -> {
                            customVariableListener.beforeEntityAdded(scoreDirector, entity);
                            customVariableListener.afterEntityAdded(scoreDirector, entity);
                        }));
    }

    @Override
    public void close() {
        customVariableListener.close();
    }

    @Override
    public String toString() {
        return customVariableListener.toString();
    }
}
