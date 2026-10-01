package ai.timefold.solver.core.impl.score.stream.collector;

final class CountHolder<Value_> {

    final Value_ value;
    long count;

    CountHolder(Value_ value) {
        this.value = value;
        this.count = 1;
    }

}
