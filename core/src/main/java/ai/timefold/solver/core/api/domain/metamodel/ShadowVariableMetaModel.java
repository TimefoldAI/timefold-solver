package ai.timefold.solver.core.api.domain.metamodel;

public interface ShadowVariableMetaModel<Solution_, Entity_> extends VariableMetaModel<Solution_, Entity_> {

    @Override
    default boolean isList() {
        return false;
    }

    @Override
    default boolean isGenuine() {
        return false;
    }

    <T> T read(Entity_ entity);

}
