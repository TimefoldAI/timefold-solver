package ai.timefold.solver.constraint.streams.bavet.common;

public interface LeftTupleLifecycle<Tuple_ extends Tuple> {

    void insertLeft(Tuple_ tuple);

    void updateLeft(Tuple_ tuple);

    void retractLeft(Tuple_ tuple);

}
