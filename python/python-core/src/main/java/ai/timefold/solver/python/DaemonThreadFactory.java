package ai.timefold.solver.python;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * There a Catch-22 that occurs on shutdown:
 * <p>
 * - In order for Python to free its variables, it must be terminated.
 * - In order for Python to be terminated, the JVM must be terminated.
 * - In order for the JVM to be terminated, all its non-daemon threads must be terminated.
 * - Executors keep all its threads alive until it is freed/have no more references.
 * - In order for the Executor to be freed/have no more references, it cannot have a reference in Python.
 * - To not have a reference in Python means Python must free its variables, creating the Catch-22
 * <p>
 * Thus, if non-daemon threads are used, and a {@link ai.timefold.solver.core.api.solver.SolverManager}
 * solves at least one problem (creating a keep-alive thread in its {@link java.util.concurrent.ThreadPoolExecutor}),
 * Python cannot shut down gracefully and will become unresponsive when interrupted.
 * <p>
 * This class uses {@link Executors#defaultThreadFactory()} to create a new thread, but sets the created
 * thread to daemon mode so Python can shut down gracefully.
 */
public class DaemonThreadFactory implements ThreadFactory {
    private static final ThreadFactory THREAD_FACTORY = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(Runnable runnable) {
        Thread out = THREAD_FACTORY.newThread(runnable);
        out.setDaemon(true);
        return out;
    }
}
