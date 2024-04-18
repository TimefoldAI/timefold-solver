package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import ai.timefold.solver.core.api.score.stream.common.Break;

/**
 * When adding fields, remember to add them to the JSON serialization code as well, if you want them exposed.
 *
 * @param <Value_>
 * @param <Point_>
 * @param <Difference_>
 */
final class BreakImpl<Value_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements Break<Value_, Difference_> {

    private final SequenceImpl<Value_, Point_, Difference_> nextSequence;
    SequenceImpl<Value_, Point_, Difference_> previousSequence;
    private Difference_ length;

    BreakImpl(SequenceImpl<Value_, Point_, Difference_> nextSequence,
            SequenceImpl<Value_, Point_, Difference_> previousSequence) {
        this.nextSequence = nextSequence;
        setPreviousSequence(previousSequence);
    }

    @Override
    public boolean isFirst() {
        return previousSequence.isFirst();
    }

    @Override
    public boolean isLast() {
        return nextSequence.isLast();
    }

    @Override
    public Value_ getPreviousSequenceEnd() {
        return previousSequence.lastItem.value();
    }

    @Override
    public Value_ getNextSequenceStart() {
        return nextSequence.firstItem.value();
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
        this.length = previousSequence.computeDifference(nextSequence);
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
