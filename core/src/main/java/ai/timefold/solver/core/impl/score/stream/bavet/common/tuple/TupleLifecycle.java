package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

public interface TupleLifecycle<Tuple_ extends AbstractTuple> {

    static <Tuple_ extends AbstractTuple> TupleLifecycle<Tuple_> ofLeft(LeftTupleLifecycle<Tuple_> leftTupleLifecycle) {
        return new LeftTupleLifecycleImpl<>(leftTupleLifecycle);
    }

    static <Tuple_ extends AbstractTuple> TupleLifecycle<Tuple_> ofRight(RightTupleLifecycle<Tuple_> rightTupleLifecycle) {
        return new RightTupleLifecycleImpl<>(rightTupleLifecycle);
    }

    static <Tuple_ extends AbstractTuple> TupleLifecycle<Tuple_> of(TupleLifecycle<Tuple_>... tupleLifecycles) {
        return new AggregatedTupleLifecycle<>(tupleLifecycles);
    }

    void insert(Tuple_ tuple);

    void update(Tuple_ tuple);

    void retract(Tuple_ tuple);

}
