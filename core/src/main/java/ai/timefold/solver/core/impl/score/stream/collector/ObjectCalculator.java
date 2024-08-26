package ai.timefold.solver.core.impl.score.stream.collector;

public sealed interface ObjectCalculator<Input_, Output_, Mapped_>
        permits ConnectedRangesCalculator, IntDistinctCountCalculator, LongDistinctCountCalculator, ReferenceAverageCalculator,
        ReferenceSumCalculator, SequenceCalculator {
    Mapped_ insert(Input_ input);

    void retract(Mapped_ mapped);

    Output_ result();
}
