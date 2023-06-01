package ai.timefold.solver.constraint.streams.bavet.common.tuple;

public interface LeftTupleLifecycle<Tuple_ extends AbstractTuple> {

    void insertLeft(Tuple_ tuple);

    void updateLeft(Tuple_ tuple);

    void retractLeft(Tuple_ tuple);

}
