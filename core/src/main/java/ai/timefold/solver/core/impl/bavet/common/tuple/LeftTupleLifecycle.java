package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface LeftTupleLifecycle<Tuple_ extends Tuple> {

    /**
     * As defined by {@link TupleLifecycle#afterAllFactsInserted}.
     */
    void afterAllFactsInsertedLeft(boolean upstreamCanProduceTuples);

    /**
     * As defined by {@link TupleLifecycle#isActive()}.
     */
    boolean isActive();

    void insertLeft(Tuple_ tuple);

    void updateLeft(Tuple_ tuple);

    void retractLeft(Tuple_ tuple);

}
