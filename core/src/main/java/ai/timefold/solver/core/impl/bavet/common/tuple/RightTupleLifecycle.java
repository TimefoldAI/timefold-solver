package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface RightTupleLifecycle<Tuple_ extends Tuple> {

    void insertRight(Tuple_ tuple);

    void updateRight(Tuple_ tuple);

    void retractRight(Tuple_ tuple);

}
