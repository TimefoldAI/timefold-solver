package ai.timefold.solver.core.impl.move.streams.dataset.joiner;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.AbstractJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataJoiner;

import org.jspecify.annotations.NullMarked;

@SuppressWarnings({ "unchecked", "rawtypes" })
@NullMarked
public final class DefaultBiDataJoiner<A, B> extends AbstractDataJoiner<B> implements BiDataJoiner<A, B> {

    private static final DefaultBiDataJoiner NONE =
            new DefaultBiDataJoiner(new Function[0], new JoinerType[0], new Function[0]);

    private final Function<A, Object>[] leftMappings;

    public <Property_> DefaultBiDataJoiner(Function<A, Property_> leftMapping, JoinerType joinerType,
            Function<B, Property_> rightMapping) {
        super(rightMapping, joinerType);
        this.leftMappings = new Function[] { leftMapping };
    }

    private <Property_> DefaultBiDataJoiner(Function<A, Property_>[] leftMappings, JoinerType[] joinerTypes,
            Function<B, Property_>[] rightMappings) {
        super(rightMappings, joinerTypes);
        this.leftMappings = (Function<A, Object>[]) leftMappings;
    }

    public static <A, B> DefaultBiDataJoiner<A, B> merge(List<DefaultBiDataJoiner<A, B>> joinerList) {
        if (joinerList.size() == 1) {
            return joinerList.get(0);
        }
        return joinerList.stream().reduce(NONE, DefaultBiDataJoiner::and);
    }

    public AbstractJoiner<B> toBiJoiner() {
        return new DefaultBiJoiner<>(leftMappings, joinerTypes, rightMappings);
    }

    @Override
    public DefaultBiDataJoiner<A, B> and(BiDataJoiner<A, B> otherJoiner) {
        var castJoiner = (DefaultBiDataJoiner<A, B>) otherJoiner;
        var joinerCount = getJoinerCount();
        var castJoinerCount = castJoiner.getJoinerCount();
        var newJoinerCount = joinerCount + castJoinerCount;
        var newJoinerTypes = Arrays.copyOf(this.joinerTypes, newJoinerCount);
        Function[] newLeftMappings = Arrays.copyOf(this.leftMappings, newJoinerCount);
        Function[] newRightMappings = Arrays.copyOf(this.rightMappings, newJoinerCount);
        for (var i = 0; i < castJoinerCount; i++) {
            var newJoinerIndex = i + joinerCount;
            newJoinerTypes[newJoinerIndex] = castJoiner.getJoinerType(i);
            newLeftMappings[newJoinerIndex] = castJoiner.getLeftMapping(i);
            newRightMappings[newJoinerIndex] = castJoiner.getRightMapping(i);
        }
        return new DefaultBiDataJoiner<>(newLeftMappings, newJoinerTypes, newRightMappings);
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
        return o instanceof DefaultBiDataJoiner<?, ?> other
                && Arrays.equals(joinerTypes, other.joinerTypes)
                && Arrays.equals(leftMappings, other.leftMappings)
                && Arrays.equals(rightMappings, other.rightMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(joinerTypes), Arrays.hashCode(leftMappings), Arrays.hashCode(rightMappings));
    }

}
