package ai.timefold.solver.service.maps.service.client.util;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.service.definition.impl.storage.inmemory.InMemoryStorage;

@ApplicationScoped
public class DummyStorage extends InMemoryStorage<DummyModelOutput> {

    @Override
    public Class<DummyModelOutput> clazz() {
        return DummyModelOutput.class;
    }
}
