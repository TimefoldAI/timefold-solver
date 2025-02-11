package ai.timefold.solver.core.preview.api.variable.provided;

public interface ShadowVariableFactory {
    <Entity_> SingleVariableReference<Entity_, Entity_> entity(Class<? extends Entity_> entityClass);

    <Entity_> ShadowCalculationBuilderFactory<Entity_> newShadow(Class<? extends Entity_> entityClass);
}
