package ai.timefold.solver.core.api.domain.metamodel;

public interface VariableMetaModel<Solution_, Entity_, Value_> {

    EntityMetaModel<Solution_, Entity_> entity();

    Class<Value_> type();

    String name();

    boolean isList();

    boolean isGenuine();

}
