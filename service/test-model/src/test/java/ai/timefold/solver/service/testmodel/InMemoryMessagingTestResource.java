package ai.timefold.solver.service.testmodel;

import java.util.HashMap;
import java.util.Map;

import ai.timefold.solver.service.definition.internal.events.SolverChannels;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

public class InMemoryMessagingTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> env = new HashMap<>();
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(SolverChannels.DATASET_COMPUTED));
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(SolverChannels.INIT_SOLUTION));
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(SolverChannels.BEST_SOLUTION));
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(SolverChannels.FINAL_BEST_SOLUTION));
        return env;
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
