package ai.timefold.solver.constraint.streams.bavet.common.tuple;

public interface RightTupleLifecycle<Tuple_ extends AbstractTuple> {

    void insertRight(Tuple_ tuple);

    void updateRight(Tuple_ tuple);

    void retractRight(Tuple_ tuple);

}
