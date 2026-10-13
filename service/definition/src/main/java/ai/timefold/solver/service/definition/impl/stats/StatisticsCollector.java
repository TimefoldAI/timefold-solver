package ai.timefold.solver.service.definition.impl.stats;

public interface StatisticsCollector {

    /**
     * @return epoch millis of the most recent start or completion of a tracked request, or 0 if there was none yet
     */
    long lastActivityTimestamp();

    /**
     * @return number of tracked requests that have started but not yet completed
     */
    int inflightRequestCount();
}
