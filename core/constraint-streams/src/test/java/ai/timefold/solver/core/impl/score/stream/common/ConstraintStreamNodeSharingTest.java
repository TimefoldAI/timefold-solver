package ai.timefold.solver.core.impl.score.stream.common;

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
    default void ifExistsOtherDifferentParent() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherDifferentParent() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherSameParentDifferentIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherSameParentDifferentIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherSameParentDifferentFilter() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherSameParentDifferentFilter() {
        // Only Unis have this variant, so don't force it.
    }

    // if(Not)ExistsOtherSameFilter cannot use node sharing, since it
    // combines its own filter with the user provided filter, creating
    // a new predicate

    default void ifExistsOtherSameParentSameIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherSameParentSameIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherIncludingUnassignedDifferentParent() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingUnassignedDifferentParent() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherIncludingUnassignedSameParentDifferentIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingUnassignedSameParentDifferentIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherIncludingUnassignedSameParentDifferentFilter() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingUnassignedSameParentDifferentFilter() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherIncludingUnassignedSameParentSameIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingUnassignedSameParentSameIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    void ifExistsDifferentParent();

    void ifNotExistsDifferentParent();

    void ifExistsIncludingUnassignedDifferentParent();

    void ifNotExistsIncludingUnassignedDifferentParent();

    void ifExistsSameParentDifferentIndexer();

    void ifExistsSameParentDifferentFilter();

    void ifNotExistsSameParentDifferentIndexer();

    void ifNotExistsSameParentDifferentFilter();

    void ifExistsIncludingUnassignedSameParentDifferentIndexer();

    void ifExistsIncludingUnassignedSameParentDifferentFilter();

    void ifNotExistsIncludingUnassignedSameParentDifferentIndexer();

    void ifNotExistsIncludingUnassignedSameParentDifferentFilter();

    void ifExistsSameParentSameIndexer();

    void ifExistsSameParentSameFilter();

    void ifNotExistsSameParentSameIndexer();

    void ifNotExistsSameParentSameFilter();

    void ifExistsIncludingUnassignedSameParentSameIndexer();

    void ifExistsIncludingUnassignedSameParentSameFilter();

    void ifNotExistsIncludingUnassignedSameParentSameIndexer();

    void ifNotExistsIncludingUnassignedSameParentSameFilter();

    // ************************************************************************
    // Group by
    // ************************************************************************

    void differentParentGroupBy();

    void differentKeyMapperGroupBy();

    void sameParentDifferentCollectorGroupBy();

    void sameParentDifferentCollectorFunctionGroupBy();

    void sameParentSameKeyMapperGroupBy();

    void sameParentSameCollectorGroupBy();

    // ************************************************************************
    // Map/expand/flatten/distinct/concat
    // ************************************************************************

    default void differentParentSameFunctionExpand() {
        // Quads don't have expand, so don't force it.
    }

    default void sameParentDifferentFunctionExpand() {
        // Quads don't have expand, so don't force it.
    }

    default void sameParentSameFunctionExpand() {
        // Quads don't have expand, so don't force it.
    }

    void differentParentSameFunctionMap();

    void sameParentDifferentFunctionMap();

    void sameParentSameFunctionMap();

    void differentParentSameFunctionFlattenLast();

    void sameParentDifferentFunctionFlattenLast();

    void sameParentSameFunctionFlattenLast();

    void differentParentDistinct();

    void sameParentDistinct();

    void differentFirstSourceConcat();

    void differentSecondSourceConcat();

    void sameSourcesConcat();
}
