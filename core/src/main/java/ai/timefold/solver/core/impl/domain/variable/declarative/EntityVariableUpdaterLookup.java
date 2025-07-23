package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriConsumer;
import ai.timefold.solver.core.impl.util.MutableReference;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class EntityVariableUpdaterLookup<Solution_> {
    private final BiFunction<Object, VariableMetaModel<Solution_, ?, ?>, Object> entityVariableToKey;
    private final Map<Object, Lookup<Solution_>> keyToEntityLookup;
    private final Supplier<Lookup<Solution_>> entityLookupSupplier;
    private int nextId = 0;

    private record Lookup<Solution_>(
            BiFunction<Object, VariableMetaModel<Solution_, ?, ?>, List<VariableUpdaterInfo<Solution_>>> getter,
            TriConsumer<Object, VariableMetaModel<Solution_, ?, ?>, Supplier<List<VariableUpdaterInfo<Solution_>>>> setter) {

        List<VariableUpdaterInfo<Solution_>> getUpdaters(Object entity, VariableMetaModel<Solution_, ?, ?> variableMetaModel) {
            return getter.apply(entity, variableMetaModel);
        }

        void setUpdaters(Object entity, VariableMetaModel<Solution_, ?, ?> variableMetaModel,
                Supplier<List<VariableUpdaterInfo<Solution_>>> updatersSupplier) {
            setter.accept(entity, variableMetaModel, updatersSupplier);
        }

    }

    private EntityVariableUpdaterLookup(Supplier<Lookup<Solution_>> entityLookupSupplier,
            BiFunction<Object, VariableMetaModel<Solution_, ?, ?>, Object> entityVariableToKey) {
        this.keyToEntityLookup = new LinkedHashMap<>();
        this.entityLookupSupplier = entityLookupSupplier;
        this.entityVariableToKey = entityVariableToKey;
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
        return new EntityVariableUpdaterLookup<>(lookupSupplier,
                (entity, variableMetamodel) -> variableMetamodel);
    }

    public static <Solution_> EntityVariableUpdaterLookup<Solution_> entityDependentLookup() {
        Supplier<Lookup<Solution_>> lookupSupplier = () -> {
            var valueMap = new IdentityHashMap<Object, List<VariableUpdaterInfo<Solution_>>>();
            return new Lookup<>((entity, ignored) -> valueMap.get(entity),
                    (entity, ignored, valueSupplier) -> valueMap.computeIfAbsent(entity, (ignored2) -> valueSupplier.get()));
        };
        return new EntityVariableUpdaterLookup<>(lookupSupplier,
                (entity, variableMetamodel) -> variableMetamodel);
    }

    public static <Solution_> EntityVariableUpdaterLookup<Solution_>
            groupedEntityDependentLookup(
                    Function<VariableMetaModel<Solution_, ?, ?>, @Nullable Function<Object, @Nullable Object>> variableToGroupKeyMapper) {
        Supplier<Lookup<Solution_>> lookupSupplier = () -> {
            var valueMap = new IdentityHashMap<Object, List<VariableUpdaterInfo<Solution_>>>();
            var groupValueMap = new LinkedHashMap<Object, List<VariableUpdaterInfo<Solution_>>>();

            BiFunction<Object, VariableMetaModel<Solution_, ?, ?>, List<VariableUpdaterInfo<Solution_>>> valueReader =
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

            TriConsumer<Object, VariableMetaModel<Solution_, ?, ?>, Supplier<List<VariableUpdaterInfo<Solution_>>>> valueSetter =
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

        return new EntityVariableUpdaterLookup<>(lookupSupplier, (entity, variableMetamodel) -> {
            var groupMapper = variableToGroupKeyMapper.apply(variableMetamodel);
            if (groupMapper != null) {
                var groupKey = groupMapper.apply(entity);
                if (groupKey != null) {
                    return new Pair<>(groupKey, variableMetamodel);
                }
            }
            return variableMetamodel;
        });
    }

    public List<VariableUpdaterInfo<Solution_>> computeUpdatersForVariableOnEntity(
            VariableMetaModel<Solution_, ?, ?> variableMetaModel,
            Object entity, Supplier<List<VariableUpdaterInfo<Solution_>>> updatersSupplier) {
        var entityLookup = keyToEntityLookup.computeIfAbsent(variableMetaModel, ignored -> entityLookupSupplier.get());
        entityLookup.setUpdaters(entity, variableMetaModel, updatersSupplier);
        return entityLookup.getUpdaters(entity, variableMetaModel);
    }

    public int getNextId() {
        return nextId++;
    }

}
