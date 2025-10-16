package ai.timefold.solver.core.impl.score.stream.common;

public interface ConstraintStreamPrecomputeTest {
    void precompute_filter_0_changed();

    default void precompute_filter_1_changed() {
        // requires two elements, so Bi, Tri and Quad
    }

    default void precompute_filter_2_changed() {
        // requires three elements, so Tri and Quad
    }

    default void precompute_filter_3_changed() {
        // requires four elements, Quad
    }

    void precompute_ifExists();

    void precompute_ifNotExists();

    void precompute_groupBy();

    void precompute_flattenLast();

    void precompute_map();

    void precompute_concat();

    void precompute_distinct();

    void precompute_complement();
}
