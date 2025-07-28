package ai.timefold.solver.core.impl.move.streams.dataset.joiner;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractDataJoiner<Right_> {

    protected final Function<Right_, Object>[] rightMappings;
    protected final JoinerType[] joinerTypes;

    protected <Property_> AbstractDataJoiner(Function<Right_, Property_> rightMapping, JoinerType joinerType) {
        this(new Function[] { rightMapping }, new JoinerType[] { joinerType });
    }

    protected <Property_> AbstractDataJoiner(Function<Right_, Property_>[] rightMappings, JoinerType[] joinerTypes) {
        this.rightMappings = (Function<Right_, Object>[]) Objects.requireNonNull(rightMappings);
        this.joinerTypes = Objects.requireNonNull(joinerTypes);
    }

    public final Function<Right_, Object> getRightMapping(int index) {
        return rightMappings[index];
    }

    public final int getJoinerCount() {
        return joinerTypes.length;
    }

    public final JoinerType getJoinerType(int index) {
        return joinerTypes[index];
    }

}
