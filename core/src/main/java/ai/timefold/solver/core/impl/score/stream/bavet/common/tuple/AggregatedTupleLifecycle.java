package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

record AggregatedTupleLifecycle<Tuple_ extends AbstractTuple>(TupleLifecycle<Tuple_>... lifecycles)
        implements
            TupleLifecycle<Tuple_> {

    @SafeVarargs
    public AggregatedTupleLifecycle {
    }

    @Override
    public void insert(Tuple_ tuple) {
        for (var lifecycle : lifecycles) {
            lifecycle.insert(tuple);
        }
    }

    @Override
    public void update(Tuple_ tuple) {
        for (var lifecycle : lifecycles) {
            lifecycle.update(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        for (var lifecycle : lifecycles) {
            lifecycle.retract(tuple);
        }
    }

    @Override
    public String toString() {
        return "size = " + lifecycles.length;
    }

}
