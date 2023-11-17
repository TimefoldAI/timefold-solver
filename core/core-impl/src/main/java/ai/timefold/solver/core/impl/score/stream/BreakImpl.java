package ai.timefold.solver.core.impl.score.stream;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors.Sequence;

final class BreakImpl<Value_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ConstraintCollectors.Break<Value_, Difference_> {

    private final BiFunction<Point_, Point_, Difference_> lengthFunction;
    private final SequenceImpl<Value_, Point_, Difference_> nextSequence;
    SequenceImpl<Value_, Point_, Difference_> previousSequence;
    private Difference_ length;

    BreakImpl(SequenceImpl<Value_, Point_, Difference_> previousSequence,
            SequenceImpl<Value_, Point_, Difference_> nextSequence, BiFunction<Point_, Point_, Difference_> lengthFunction) {
        this.lengthFunction = lengthFunction;
        this.nextSequence = nextSequence;
        setPreviousSequence(previousSequence);
    }

    @Override
    public Sequence<Value_, Difference_> getPreviousSequence() {
        return previousSequence;
    }

    @Override
    public Sequence<Value_, Difference_> getNextSequence() {
        return nextSequence;
    }

    @Override
    public Difference_ getLength() {
        return length;
    }

    void setPreviousSequence(SequenceImpl<Value_, Point_, Difference_> previousSequence) {
        this.previousSequence = previousSequence;
        updateLength();
    }

    void updateLength() {
        this.length = lengthFunction.apply(previousSequence.lastItem.index(), nextSequence.firstItem.index());
    }

    @Override
    public String toString() {
        return "Break{" +
                "previousSequence=" + previousSequence +
                ", nextSequence=" + nextSequence +
                ", length=" + length +
                '}';
    }
}
