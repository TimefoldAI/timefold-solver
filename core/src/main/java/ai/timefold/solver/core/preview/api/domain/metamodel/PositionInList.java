package ai.timefold.solver.core.preview.api.domain.metamodel;

import ai.timefold.solver.core.api.domain.common.Lookup;

import org.jspecify.annotations.NullMarked;

/**
 * Uniquely identifies the position of a value in a list variable.
 * Instances can be created by {@link ElementPosition#of(Object, int)}.
 * <p>
 * Within that one list, the index is unique for each value and therefore the instances are comparable.
 * Comparing them between different lists has no meaning.
 */
@NullMarked
public sealed interface PositionInList
        extends ElementPosition, Comparable<PositionInList>
        permits DefaultPositionInList {

    <Entity_> Entity_ entity();

    int index();

    /**
     * Rebase this position in list to a new thread.
     * This is a convenience method that calls {@link Lookup#lookUpNonNullWorkingObject(Object)}.
     *
     * @param lookup the helper
     * @return the rebased position
     * @see Lookup Description of rebasing.
     */
    PositionInList rebase(Lookup lookup);

}
