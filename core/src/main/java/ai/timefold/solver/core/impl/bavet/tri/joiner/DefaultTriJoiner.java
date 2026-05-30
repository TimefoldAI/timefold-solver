package ai.timefold.solver.core.impl.bavet.tri.joiner;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.AbstractJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultTriJoiner<A, B, C>
        extends AbstractJoiner<C>
        implements TriJoiner<A, B, C> {

    private static final DefaultTriJoiner NONE = new DefaultTriJoiner(new BiFunction[0], new JoinerType[0], new Function[0]);

    private final BiFunction<A, B, Object>[] leftMappings;

    public DefaultTriJoiner(BiFunction<A, B, ?> leftMapping, JoinerType joinerType, Function<C, ?> rightMapping) {
        super(rightMapping, joinerType);
        this.leftMappings = new BiFunction[] { leftMapping };
    }

    public DefaultTriJoiner(BiFunction<A, B, ?>[] leftMappings, JoinerType[] joinerTypes, Function<C, ?>[] rightMappings) {
        super(rightMappings, joinerTypes);
        this.leftMappings = (BiFunction<A, B, Object>[]) Objects.requireNonNull(leftMappings);
    }

    public static <A, B, C> DefaultTriJoiner<A, B, C> merge(List<DefaultTriJoiner<A, B, C>> joinerList) {
        return switch (joinerList.size()) {
            case 0 -> NONE;
            case 1 -> joinerList.getFirst();
            default -> joinerList.stream().reduce(NONE, DefaultTriJoiner::and);
        };
    }

    @Override
    public DefaultTriJoiner<A, B, C> and(TriJoiner<A, B, C> otherJoiner) {
        var castJoiner = (DefaultTriJoiner<A, B, C>) otherJoiner;
        var joinerCount = getJoinerCount();
        var castJoinerCount = castJoiner.getJoinerCount();
        var newJoinerCount = joinerCount + castJoinerCount;
        var newJoinerTypes = Arrays.copyOf(this.joinerTypes, newJoinerCount);
        var newLeftMappings = Arrays.copyOf(this.leftMappings, newJoinerCount);
        var newRightMappings = Arrays.copyOf(this.rightMappings, newJoinerCount);
        for (var i = 0; i < castJoinerCount; i++) {
            var newJoinerIndex = i + joinerCount;
            newJoinerTypes[newJoinerIndex] = castJoiner.getJoinerType(i);
            newLeftMappings[newJoinerIndex] = castJoiner.getLeftMapping(i);
            newRightMappings[newJoinerIndex] = castJoiner.getRightMapping(i);
        }
        return new DefaultTriJoiner<>(newLeftMappings, newJoinerTypes, newRightMappings);
    }

    /**
     * @return this if already equal-first (or single joiner); otherwise a copy with all
     *         {@link JoinerType#EQUAL} joiners moved to the front (stable, see
     *         {@link AbstractJoiner#equalsFirstOrder}).
     */
    DefaultTriJoiner<A, B, C> reorderedEqualsFirst() {
        var order = equalsFirstOrder(joinerTypes);
        if (order == null) {
            return this;
        }
        var count = order.length;
        var newLeftMappings = new BiFunction[count];
        var newJoinerTypes = new JoinerType[count];
        var newRightMappings = new Function[count];
        for (var i = 0; i < count; i++) {
            var from = order[i];
            newLeftMappings[i] = leftMappings[from];
            newJoinerTypes[i] = joinerTypes[from];
            newRightMappings[i] = rightMappings[from];
        }
        return new DefaultTriJoiner<A, B, C>(newLeftMappings, newJoinerTypes, newRightMappings);
    }

    public BiFunction<A, B, Object> getLeftMapping(int index) {
        return leftMappings[index];
    }

    public boolean matches(A a, B b, C c) {
        var joinerCount = getJoinerCount();
        for (var i = 0; i < joinerCount; i++) {
            var joinerType = getJoinerType(i);
            var leftMapping = getLeftMapping(i).apply(a, b);
            var rightMapping = getRightMapping(i).apply(c);
            if (!joinerType.matches(leftMapping, rightMapping)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultTriJoiner<?, ?, ?> other) {
            return Arrays.equals(joinerTypes, other.joinerTypes)
                    && Arrays.equals(leftMappings, other.leftMappings)
                    && Arrays.equals(rightMappings, other.rightMappings);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(joinerTypes), Arrays.hashCode(leftMappings), Arrays.hashCode(rightMappings));
    }

}
