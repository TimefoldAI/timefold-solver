package ai.timefold.solver.core.impl.bavet.quad.joiner;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.AbstractJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultQuadJoiner<A, B, C, D>
        extends AbstractJoiner<D>
        implements QuadJoiner<A, B, C, D> {

    private static final DefaultQuadJoiner NONE = new DefaultQuadJoiner(new TriFunction[0], new JoinerType[0], new Function[0]);

    private final TriFunction<A, B, C, Object>[] leftMappings;

    public DefaultQuadJoiner(TriFunction<A, B, C, ?> leftMapping, JoinerType joinerType, Function<D, ?> rightMapping) {
        super(rightMapping, joinerType);
        this.leftMappings = new TriFunction[] { leftMapping };
    }

    public DefaultQuadJoiner(TriFunction<A, B, C, ?>[] leftMappings, JoinerType[] joinerTypes, Function<D, ?>[] rightMappings) {
        super(rightMappings, joinerTypes);
        this.leftMappings = (TriFunction<A, B, C, Object>[]) Objects.requireNonNull(leftMappings);
    }

    public static <A, B, C, D> DefaultQuadJoiner<A, B, C, D> merge(List<DefaultQuadJoiner<A, B, C, D>> joinerList) {
        return switch (joinerList.size()) {
            case 0 -> NONE;
            case 1 -> joinerList.getFirst();
            default -> joinerList.stream().reduce(NONE, DefaultQuadJoiner::and);
        };
    }

    @Override
    public DefaultQuadJoiner<A, B, C, D> and(QuadJoiner<A, B, C, D> otherJoiner) {
        var castJoiner = (DefaultQuadJoiner<A, B, C, D>) otherJoiner;
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
        return new DefaultQuadJoiner<>(newLeftMappings, newJoinerTypes, newRightMappings);
    }

    /**
     * @return this if already equal-first (or single joiner); otherwise a copy with all
     *         {@link JoinerType#EQUAL} joiners moved to the front (stable, see
     *         {@link AbstractJoiner#equalsFirstOrder}).
     */
    DefaultQuadJoiner<A, B, C, D> reorderedEqualsFirst() {
        var order = equalsFirstOrder(joinerTypes);
        if (order == null) {
            return this;
        }
        var count = order.length;
        var newLeftMappings = new TriFunction[count];
        var newJoinerTypes = new JoinerType[count];
        var newRightMappings = new Function[count];
        for (var i = 0; i < count; i++) {
            var from = order[i];
            newLeftMappings[i] = leftMappings[from];
            newJoinerTypes[i] = joinerTypes[from];
            newRightMappings[i] = rightMappings[from];
        }
        return new DefaultQuadJoiner<A, B, C, D>(newLeftMappings, newJoinerTypes, newRightMappings);
    }

    public TriFunction<A, B, C, Object> getLeftMapping(int index) {
        return leftMappings[index];
    }

    public boolean matches(A a, B b, C c, D d) {
        var joinerCount = getJoinerCount();
        for (var i = 0; i < joinerCount; i++) {
            var joinerType = getJoinerType(i);
            var leftMapping = getLeftMapping(i).apply(a, b, c);
            var rightMapping = getRightMapping(i).apply(d);
            if (!joinerType.matches(leftMapping, rightMapping)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultQuadJoiner<?, ?, ?, ?> other) {
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
