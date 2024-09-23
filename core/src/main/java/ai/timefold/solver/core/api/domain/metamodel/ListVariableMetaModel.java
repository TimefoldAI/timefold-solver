package ai.timefold.solver.core.api.domain.metamodel;

import java.util.List;

public interface ListVariableMetaModel<Solution_, Entity_> extends VariableMetaModel<Solution_, Entity_> {

    @Override
    default boolean isList() {
        return true;
    }

    @Override
    default boolean isGenuine() {
        return true;
    }

    boolean allowsUnassignedValues();

    <Value_> List<Value_> read(Entity_ entity);

    LocationInList<Entity_> positionOf(Object value);

    default <Value_> List<Value_> write(Entity_ entity, List<Value_> value) {
        return write(entity, value, 0);
    }

    default <Value_> List<Value_> write(Entity_ entity, List<Value_> value, int fromIndex) {
        return write(entity, value, fromIndex, value.size());
    }

    <Value_> List<Value_> write(Entity_ entity, List<Value_> value, int fromIndex, int toIndex);

    <Value_> Value_ removeValue(Entity_ entity, int index);

    <Value_> Value_ replaceValue(Entity_ entity, int index, Value_ value);

    <Value_> void insertValue(Entity_ entity, int index, Value_ value);

    record LocationInList<Entity_>(Entity_ entity, int index) {

    }

}
