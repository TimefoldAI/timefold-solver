package ai.timefold.solver.core.api.domain.metamodel;

public interface ListVariableMetaModel<Solution_, Entity_, Value_> extends VariableMetaModel<Solution_, Entity_, Value_> {

    @Override
    default boolean isList() {
        return true;
    }

    @Override
    default boolean isGenuine() {
        return true;
    }

    boolean allowsUnassignedValues();

}
