package ai.timefold.solver.service.worker.impl.testdata;

import ai.timefold.solver.service.definition.impl.storage.inmemory.InMemoryStorage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Real, in-memory-backed {@code Storage} used by tests instead of a mock, so that reads reflect what was
 * actually written rather than a stubbed expectation.
 */
public class TestdataStorage extends InMemoryStorage<TestdataModelOutput> {

    public TestdataStorage(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    public Class<TestdataModelOutput> clazz() {
        return TestdataModelOutput.class;
    }
}