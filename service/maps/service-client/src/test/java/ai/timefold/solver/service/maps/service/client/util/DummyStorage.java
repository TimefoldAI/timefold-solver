package ai.timefold.solver.service.maps.service.client.util;

import jakarta.inject.Singleton;

import ai.timefold.solver.service.definition.impl.storage.inmemory.InMemoryStorage;

@Singleton
public class DummyStorage extends InMemoryStorage<DummyModelOutput> {

    @Override
    public Class<DummyModelOutput> clazz() {
        return DummyModelOutput.class;
    }
}
