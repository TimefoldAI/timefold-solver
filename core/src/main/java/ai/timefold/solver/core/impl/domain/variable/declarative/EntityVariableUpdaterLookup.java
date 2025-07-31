package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.MutableReference;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class EntityVariableUpdaterLookup<Solution_> {
    private final Map<Object, Lookup<Solution_>> variableToEntityLookup;
    private final Supplier<Lookup<Solution_>> entityLookupSupplier;
    private int nextId = 0;

    private interface LookupGetter<Solution_> {
        List<VariableUpdaterInfo<Solution_>> get(Object entity, VariableMetaModel<Solution_, ?, ?> variableMetaModel);
    }

    private interface LookupSetter<Solution_> {
        void putIfMissing(Object entity, VariableMetaModel<Solution_, ?, ?> variableMetaModel,
                Supplier<List<VariableUpdaterInfo<Solution_>>> valueSupplier);
    }

    private record Lookup<Solution_>(
            LookupGetter<Solution_> getter,
            LookupSetter<Solution_> setter) {

        List<VariableUpdaterInfo<Solution_>> getUpdaters(Object entity, VariableMetaModel<Solution_, ?, ?> variableMetaModel) {
            return getter.get(entity, variableMetaModel);
        }

        void setUpdaters(Object entity, VariableMetaModel<Solution_, ?, ?> variableMetaModel,
                Supplier<List<VariableUpdaterInfo<Solution_>>> updatersSupplier) {
            setter.putIfMissing(entity, variableMetaModel, updatersSupplier);
        }

    }

    private EntityVariableUpdaterLookup(Supplier<Lookup<Solution_>> entityLookupSupplier) {
        this.variableToEntityLookup = new LinkedHashMap<>();
        this.entityLookupSupplier = entityLookupSupplier;
    }

    public static <Solution_> EntityVariableUpdaterLookup<Solution_> entityIndependentLookup() {
        Supplier<Lookup<Solution_>> lookupSupplier = () -> {
            var sharedValue = new MutableReference<List<VariableUpdaterInfo<Solution_>>>(null);
            return new Lookup<>((ignored1, ignored2) -> sharedValue.getValue(),
                    (ignored1, ignored2, valueSupplier) -> {
                        if (sharedValue.getValue() == null) {
                            sharedValue.setValue(valueSupplier.get());
                        }
                    });
        };
        return new EntityVariableUpdaterLookup<>(lookupSupplier);
    }

    public static <Solution_> EntityVariableUpdaterLookup<Solution_> entityDependentLookup() {
        Supplier<Lookup<Solution_>> lookupSupplier = () -> {
            var valueMap = new IdentityHashMap<Object, List<VariableUpdaterInfo<Solution_>>>();
            return new Lookup<>((entity, ignored) -> valueMap.get(entity),
                    (entity, ignored, valueSupplier) -> valueMap.computeIfAbsent(entity, ignored2 -> valueSupplier.get()));
        };
        return new EntityVariableUpdaterLookup<>(lookupSupplier);
    }

    public static <Solution_> EntityVariableUpdaterLookup<Solution_>
            groupedEntityDependentLookup(
                    Function<VariableMetaModel<Solution_, ?, ?>, @Nullable Function<Object, @Nullable Object>> variableToGroupKeyMapper) {
        Supplier<Lookup<Solution_>> lookupSupplier = () -> {
            var valueMap = new IdentityHashMap<Object, List<VariableUpdaterInfo<Solution_>>>();
            var groupValueMap = new LinkedHashMap<Object, List<VariableUpdaterInfo<Solution_>>>();

            LookupGetter<Solution_> valueReader =
                    (entity, variableMetaModel) -> {
                        var groupMapper = variableToGroupKeyMapper.apply(variableMetaModel);
                        if (groupMapper != null) {
                            var groupKey = groupMapper.apply(entity);
                            if (groupKey != null) {
                                return groupValueMap.get(groupKey);
                            }
                        }
                        return valueMap.get(entity);
                    };

            LookupSetter<Solution_> valueSetter =
                    (entity, variableMetaModel, valueSupplier) -> {
                        var groupMapper = variableToGroupKeyMapper.apply(variableMetaModel);
                        if (groupMapper != null) {
                            var groupKey = groupMapper.apply(entity);
                            if (groupKey != null) {
                                groupValueMap.computeIfAbsent(groupKey, ignored -> valueSupplier.get());
                                return;
                            }
                        }
                        valueMap.computeIfAbsent(entity, ignored -> valueSupplier.get());
                    };
            return new Lookup<>(valueReader,
                    valueSetter);
        };

        return new EntityVariableUpdaterLookup<>(lookupSupplier);
    }

    public List<VariableUpdaterInfo<Solution_>> computeUpdatersForVariableOnEntity(
            VariableMetaModel<Solution_, ?, ?> variableMetaModel,
            Object entity, Supplier<List<VariableUpdaterInfo<Solution_>>> updatersSupplier) {
        var entityLookup = variableToEntityLookup.computeIfAbsent(variableMetaModel, ignored -> entityLookupSupplier.get());
        entityLookup.setUpdaters(entity, variableMetaModel, updatersSupplier);
        return entityLookup.getUpdaters(entity, variableMetaModel);
    }

    public int getNextId() {
        return nextId++;
    }

}
