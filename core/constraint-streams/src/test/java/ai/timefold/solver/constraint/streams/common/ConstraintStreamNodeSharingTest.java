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

    default void ifExistsOtherIncludingNullVarsDifferentParent() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingNullVarsDifferentParent() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherIncludingNullVarsSameParentDifferentIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingNullVarsSameParentDifferentIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherIncludingNullVarsSameParentDifferentFilter() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingNullVarsSameParentDifferentFilter() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifExistsOtherIncludingNullVarsSameParentSameIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    default void ifNotExistsOtherIncludingNullVarsSameParentSameIndexer() {
        // Only Unis have this variant, so don't force it.
    }

    void ifExistsDifferentParent();

    void ifNotExistsDifferentParent();

    void ifExistsIncludingNullVarsDifferentParent();

    void ifNotExistsIncludingNullVarsDifferentParent();

    void ifExistsSameParentDifferentIndexer();

    void ifExistsSameParentDifferentFilter();

    void ifNotExistsSameParentDifferentIndexer();

    void ifNotExistsSameParentDifferentFilter();

    void ifExistsIncludingNullVarsSameParentDifferentIndexer();

    void ifExistsIncludingNullVarsSameParentDifferentFilter();

    void ifNotExistsIncludingNullVarsSameParentDifferentIndexer();

    void ifNotExistsIncludingNullVarsSameParentDifferentFilter();

    void ifExistsSameParentSameIndexer();

    void ifExistsSameParentSameFilter();

    void ifNotExistsSameParentSameIndexer();

    void ifNotExistsSameParentSameFilter();

    void ifExistsIncludingNullVarsSameParentSameIndexer();

    void ifExistsIncludingNullVarsSameParentSameFilter();

    void ifNotExistsIncludingNullVarsSameParentSameIndexer();

    void ifNotExistsIncludingNullVarsSameParentSameFilter();

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
