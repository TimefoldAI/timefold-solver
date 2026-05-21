package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public abstract class AbstractLoadBalanceSlot<Balanced_> {

    private final DefaultLoadBalance<Balanced_> container;
    private @Nullable Balanced_ cachedBalanced;
    private long cachedLoad;

    public AbstractLoadBalanceSlot(DefaultLoadBalance<Balanced_> container) {
        this.container = container;
    }

    protected void addMapped(Balanced_ balanced, long load, long initialLoad) {
        cachedBalanced = balanced;
        cachedLoad = load;
        container.registerBalanced(balanced, load, initialLoad);
    }

    protected void replaceWithMapped(Balanced_ balanced, long load, long initialLoad) {
        if (Objects.equals(cachedBalanced, balanced) && cachedLoad == load) {
            return;
        }
        removeMapped();
        addMapped(balanced, load, initialLoad);
    }

    protected void removeMapped() {
        container.unregisterBalanced(cachedBalanced, cachedLoad);
    }
}
