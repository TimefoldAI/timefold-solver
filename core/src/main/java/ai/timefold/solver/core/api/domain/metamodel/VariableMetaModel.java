package ai.timefold.solver.core.api.domain.metamodel;

public interface VariableMetaModel<Solution_, Entity_> {

    EntityMetaModel<Solution_, Entity_> entity();

    String name();

    Class<?> type();

    boolean isList();

    boolean isGenuine();

}
