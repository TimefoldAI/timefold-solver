package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

import org.jspecify.annotations.NullMarked;

/**
 * A contiguous span of positions in a list variable,
 * drawn together to be moved as one.
 * Unlike {@link Sample}, which is an unordered member set with no key,
 * a {@code Range} is positionally identified:
 * two ranges are equal only when they point at the same entity and the same {@code [fromIndex, toIndex)} span.
 * <p>
 * A range is immutable and safe to hold in a move,
 * but it says nothing about the solution,
 * so every value a move needs must be read from the live solution.
 * Instances are produced by {@link SubListSampler}.
 *
 * @param entity never null
 * @param fromIndex inclusive, 0 or higher
 * @param toIndex exclusive, greater than {@code fromIndex}
 * @param <Entity_> the type of the entity whose list variable this range points into
 */
@NullMarked
public record Range<Entity_>(Entity_ entity, int fromIndex, int toIndex) {

    /**
     * @param from the start of the range, inclusive
     * @param length 1 or higher
     * @return never null
     */
    public static <Entity_> Range<Entity_> of(PositionInList from, int length) {
        return new Range<>(from.entity(), from.index(), from.index() + length);
    }

    public Range(Entity_ entity, int fromIndex, int toIndex) {
        this.entity = Objects.requireNonNull(entity, "entity");
        if (fromIndex < 0) {
            throw new IllegalArgumentException("The fromIndex (%d) must not be negative."
                    .formatted(fromIndex));
        }
        if (toIndex <= fromIndex) {
            throw new IllegalArgumentException("The toIndex (%d) must be greater than the fromIndex (%d)."
                    .formatted(toIndex, fromIndex));
        }
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /**
     * @return {@link #toIndex()} minus {@link #fromIndex()}; always 1 or higher
     */
    public int length() {
        return toIndex - fromIndex;
    }

    public Range<Entity_> rebase(Lookup lookup) {
        return new Range<>(lookup.lookUpNonNullWorkingObject(entity), fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return entity + "[" + fromIndex + ".." + (toIndex - 1) + "]";
    }

}
