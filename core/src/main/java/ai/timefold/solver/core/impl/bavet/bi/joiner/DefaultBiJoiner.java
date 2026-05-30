package ai.timefold.solver.core.impl.bavet.bi.joiner;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.AbstractJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiJoiner<A, B>
        extends AbstractJoiner<B>
        implements BiJoiner<A, B> {

    private static final DefaultBiJoiner NONE = new DefaultBiJoiner(new Function[0], new JoinerType[0], new Function[0]);

    private final Function<A, Object>[] leftMappings;

    public DefaultBiJoiner(Function<A, ?> leftMapping, JoinerType joinerType, Function<B, ?> rightMapping) {
        super(rightMapping, joinerType);
        this.leftMappings = new Function[] { leftMapping };
    }

    public DefaultBiJoiner(Function<A, ?>[] leftMappings, JoinerType[] joinerTypes, Function<B, ?>[] rightMappings) {
        super(rightMappings, joinerTypes);
        this.leftMappings = (Function<A, Object>[]) Objects.requireNonNull(leftMappings);
    }

    public static <A, B> DefaultBiJoiner<A, B> merge(List<DefaultBiJoiner<A, B>> joinerList) {
        return switch (joinerList.size()) {
            case 0 -> NONE;
            case 1 -> joinerList.getFirst();
            default -> joinerList.stream().reduce(NONE, DefaultBiJoiner::and);
        };
    }

    @Override
    public DefaultBiJoiner<A, B> and(BiJoiner<A, B> otherJoiner) {
        var castJoiner = (DefaultBiJoiner<A, B>) otherJoiner;
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
        return new DefaultBiJoiner<>(newLeftMappings, newJoinerTypes, newRightMappings);
    }

    /**
     * @return this if already equal-first (or single joiner); otherwise a copy with all
     *         {@link JoinerType#EQUAL} joiners moved to the front (stable, see
     *         {@link AbstractJoiner#equalsFirstOrder}).
     */
    public DefaultBiJoiner<A, B> reorderedEqualsFirst() {
        var order = equalsFirstOrder(joinerTypes);
        if (order == null) {
            return this;
        }
        var count = order.length;
        var newLeftMappings = new Function[count];
        var newJoinerTypes = new JoinerType[count];
        var newRightMappings = new Function[count];
        for (var i = 0; i < count; i++) {
            var from = order[i];
            newLeftMappings[i] = leftMappings[from];
            newJoinerTypes[i] = joinerTypes[from];
            newRightMappings[i] = rightMappings[from];
        }
        return new DefaultBiJoiner<A, B>(newLeftMappings, newJoinerTypes, newRightMappings);
    }

    public Function<A, Object> getLeftMapping(int index) {
        return leftMappings[index];
    }

    public boolean matches(A a, B b) {
        var joinerCount = getJoinerCount();
        for (var i = 0; i < joinerCount; i++) {
            var joinerType = getJoinerType(i);
            var leftMapping = getLeftMapping(i).apply(a);
            var rightMapping = getRightMapping(i).apply(b);
            if (!joinerType.matches(leftMapping, rightMapping)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultBiJoiner<?, ?> other) {
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
