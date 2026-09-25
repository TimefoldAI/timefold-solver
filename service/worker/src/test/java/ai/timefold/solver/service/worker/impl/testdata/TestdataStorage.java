package ai.timefold.solver.service.worker.impl.testdata;

import ai.timefold.solver.service.storage.inmemory.InMemoryStorage;

/**
 * Real, in-memory-backed {@code Storage} used by tests instead of a mock, so that reads reflect what was
 * actually written rather than a stubbed expectation.
 */
public class TestdataStorage extends InMemoryStorage {
}
