package ai.timefold.solver.core.impl.score.stream.collector;

public sealed interface ObjectCalculator<Input_>
        permits ConnectedRangesCalculator, LongDistinctCountCalculator, ReferenceAverageCalculator, ReferenceSumCalculator,
        SequenceCalculator {
    void insert(Input_ input);

    void update(Input_ input);

    void retract();
}
