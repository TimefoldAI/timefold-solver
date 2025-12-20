package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface LeftTupleLifecycle<Tuple_ extends Tuple> {

    void insertLeft(Tuple_ tuple);

    void updateLeft(Tuple_ tuple);

    void retractLeft(Tuple_ tuple);

}
