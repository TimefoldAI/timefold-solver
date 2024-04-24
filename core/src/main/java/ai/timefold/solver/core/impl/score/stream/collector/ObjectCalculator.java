package ai.timefold.solver.core.impl.score.stream.collector;

public sealed interface ObjectCalculator<Input_, Output_>
        permits ConnectedRangesCalculator, IntDistinctCountCalculator, LongDistinctCountCalculator, ReferenceAverageCalculator,
        ReferenceSumCalculator, SequenceCalculator {
    void insert(Input_ input);

    void retract(Input_ input);

    Output_ result();
}
