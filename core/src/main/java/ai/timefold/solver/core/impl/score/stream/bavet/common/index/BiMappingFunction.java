package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.score.stream.common.AbstractJoiner;
import ai.timefold.solver.core.impl.score.stream.common.tri.DefaultTriJoiner;

@FunctionalInterface
interface BiMappingFunction<A, B> extends BiFunction<A, B, Object> {

    static <A, B> BiMappingFunction<A, B> of(AbstractJoiner<?> joiner, int index) {
        var castJoiner = (DefaultTriJoiner<A, B, ?>) joiner;
        var mapping = castJoiner.getLeftMapping(index);
        return mapping::apply;
    }

}
