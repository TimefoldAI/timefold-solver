package ai.timefold.solver.benchmark.impl.statistic;

public interface StatisticType {

    /**
     * @return never null
     */
    String name();

    /**
     * @return never null
     */
    default String getLabel() {
        return name().replace('_', ' ');
    }

}
