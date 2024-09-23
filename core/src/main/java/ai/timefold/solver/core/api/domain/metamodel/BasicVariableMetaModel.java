package ai.timefold.solver.core.api.domain.metamodel;

public interface BasicVariableMetaModel<Solution_, Entity_> extends VariableMetaModel<Solution_, Entity_> {

    @Override
    default boolean isList() {
        return false;
    }

    @Override
    default boolean isGenuine() {
        return true;
    }

    boolean allowsUnassigned();

    <T> T read(Entity_ entity);

    <T> T write(Entity_ entity, Object value);

}
