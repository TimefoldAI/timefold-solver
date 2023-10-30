package ai.timefold.solver.constraint.streams.common;

/**
 * Defines methods that every constraint stream node sharing test must have. These methods are enforced because they test each
 * method of the API for correctness.
 */
public interface ConstraintStreamNodeSharingTest {

    // ************************************************************************
    // Filter
    // ************************************************************************
    void differentParentSameFilter();

    void sameParentDifferentFilter();

    void sameParentSameFilter();

    // ************************************************************************
    // Join
    // ************************************************************************
    default void differentLeftParentJoin() {
        // Quads don't have joins, so don't force it.
    }

    default void differentRightParentJoin() {
        // Quads don't have joins, so don't force it.
    }

    default void sameParentsDifferentIndexerJoin() {
        // Quads don't have joins, so don't force it.
    }

    default void sameParentsDifferentFilteringJoin() {
        // Quads don't have joins, so don't force it.
    }

    default void sameParentsJoin() {
        // Quads don't have joins, so don't force it.
    }

    default void sameParentsSameIndexerJoin() {
        // Quads don't have joins, so don't force it.
    }

    default void sameParentsSameFilteringJoin() {
        // Quads don't have joins, so don't force it.
    }

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    // TODO

    // ************************************************************************
    // Group by
    // ************************************************************************

    // TODO

    // ************************************************************************
    // Map/expand/flatten/distinct/concat
    // ************************************************************************

    // TODO Map/expand/flatten

    void differentParentDistinct();

    void sameParentDistinct();

    void differentFirstSourceConcat();

    void differentSecondSourceConcat();

    void sameSourcesConcat();
}
