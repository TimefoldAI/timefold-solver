package ai.timefold.solver.core.impl.solver.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NullMarked;

/**
 * Similar to {@link Executors}'s DefaultThreadFactory, but allows settings a namePrefix.
 */
@NullMarked
public final class DefaultSolverThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public DefaultSolverThreadFactory(String threadPrefix) {
        namePrefix = "Timefold-%d-%s-"
                .formatted(poolNumber.incrementAndGet(), threadPrefix);
    }

    @Override
    public Thread newThread(Runnable r) {
        return Thread.ofPlatform()
                .name(namePrefix + threadNumber.getAndIncrement())
                .group(Thread.currentThread().getThreadGroup())
                .priority(Thread.NORM_PRIORITY)
                .daemon(false)
                .unstarted(r);
    }

}
