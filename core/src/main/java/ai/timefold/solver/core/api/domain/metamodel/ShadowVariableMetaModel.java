package ai.timefold.solver.core.api.domain.metamodel;

public interface ShadowVariableMetaModel<Solution_, Entity_, Value_>
        extends VariableMetaModel<Solution_, Entity_, Value_> {

    @Override
    default boolean isList() {
        return false;
    }

    @Override
    default boolean isGenuine() {
        return false;
    }

}
