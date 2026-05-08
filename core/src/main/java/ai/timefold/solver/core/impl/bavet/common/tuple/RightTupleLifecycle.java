package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface RightTupleLifecycle<Tuple_ extends Tuple> {

    /**
     * As defined by {@link TupleLifecycle#initialize}.
     */
    void initializeRight(boolean upstreamCanProduceTuples);

    /**
     * As defined by {@link TupleLifecycle#isActive()}.
     */
    boolean isActive();

    void insertRight(Tuple_ tuple);

    void updateRight(Tuple_ tuple);

    void retractRight(Tuple_ tuple);

}
