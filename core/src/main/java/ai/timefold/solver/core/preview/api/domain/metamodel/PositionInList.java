package ai.timefold.solver.core.preview.api.domain.metamodel;

import org.jspecify.annotations.NullMarked;

/**
 * Uniquely identifies the position of a value in a list variable.
 * Instances can be created by {@link ElementPosition#of(Object, int)}.
 * <p>
 * Within that one list, the index is unique for each value and therefore the instances are comparable.
 * Comparing them between different lists has no meaning.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 *
 */
@NullMarked
public sealed interface PositionInList
        extends ElementPosition, Comparable<PositionInList>
        permits DefaultPositionInList {

    <Entity_> Entity_ entity();

    int index();

}
