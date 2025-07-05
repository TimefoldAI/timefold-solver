package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.MutableReference;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

public class EntityVariableUpdaterLookup<Solution_> {
    private final Map<VariableMetaModel<?, ?, ?>, Lookup<Solution_>> variableToEntityLookup;
    private final Supplier<Lookup<Solution_>> entityLookupSupplier;
    private int nextId = 0;

    private record Lookup<Solution_>(Function<Object, List<VariableUpdaterInfo<Solution_>>> getter,
            BiConsumer<Object, Supplier<List<VariableUpdaterInfo<Solution_>>>> setter) {

        List<VariableUpdaterInfo<Solution_>> getUpdaters(Object entity) {
            return getter.apply(entity);
        }

        void setUpdaters(Object entity, Supplier<List<VariableUpdaterInfo<Solution_>>> updatersSupplier) {
            setter.accept(entity, updatersSupplier);
        }

    }

    private EntityVariableUpdaterLookup(Supplier<Lookup<Solution_>> entityLookupSupplier) {
        this.variableToEntityLookup = new LinkedHashMap<>();
        this.entityLookupSupplier = entityLookupSupplier;
    }

    public static <Solution_> EntityVariableUpdaterLookup<Solution_> entityIndependentLookup() {
        Supplier<Lookup<Solution_>> lookupSupplier = () -> {
            var sharedValue = new MutableReference<List<VariableUpdaterInfo<Solution_>>>(null);
            return new Lookup<>(ignored -> sharedValue.getValue(),
                    (ignored, valueSupplier) -> {
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
            return new Lookup<>(valueMap::get,
                    (entity, valueSupplier) -> valueMap.computeIfAbsent(entity, ignored -> valueSupplier.get()));
        };
        return new EntityVariableUpdaterLookup<>(lookupSupplier);
    }

    public List<VariableUpdaterInfo<Solution_>> computeUpdatersForVariableOnEntity(VariableMetaModel<?, ?, ?> variableMetaModel,
            Object entity, Supplier<List<VariableUpdaterInfo<Solution_>>> updatersSupplier) {
        var entityLookup = variableToEntityLookup.computeIfAbsent(variableMetaModel, ignored -> entityLookupSupplier.get());
        entityLookup.setUpdaters(entity, updatersSupplier);
        return entityLookup.getUpdaters(entity);
    }

    public int getNextId() {
        return nextId++;
    }

}
