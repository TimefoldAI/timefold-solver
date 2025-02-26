package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ShadowVariableFactory {
    <Entity_> SingleVariableReference<Entity_, Entity_> entity(Class<? extends Entity_> entityClass);

    <Entity_> ShadowCalculationBuilderFactory<Entity_> newShadow(Class<? extends Entity_> entityClass);
}
