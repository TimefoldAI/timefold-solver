package ai.timefold.solver.core.impl.score.stream;

import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;

/**
 * Used via {@link ServiceLoader} so that the constraint streams implementation can be fully split from its API,
 * without getting split packages or breaking backwards compatibility.
 */
public interface JoinerService {

    /*
     * TODO In 9.x, bring API and impl to the same JAR, avoiding split packages.
     * This will make this SPI unnecessary.
     */

    <A, B> BiJoiner<A, B> newBiJoiner(BiPredicate<A, B> filter);

    <A, B, Property_> BiJoiner<A, B> newBiJoiner(Function<A, Property_> leftMapping, JoinerType joinerType,
            Function<B, Property_> rightMapping);

    <A, B, C> TriJoiner<A, B, C> newTriJoiner(TriPredicate<A, B, C> filter);

    <A, B, C, Property_> TriJoiner<A, B, C> newTriJoiner(BiFunction<A, B, Property_> leftMapping, JoinerType joinerType,
            Function<C, Property_> rightMapping);

    <A, B, C, D> QuadJoiner<A, B, C, D> newQuadJoiner(QuadPredicate<A, B, C, D> filter);

    <A, B, C, D, Property_> QuadJoiner<A, B, C, D> newQuadJoiner(TriFunction<A, B, C, Property_> leftMapping,
            JoinerType joinerType, Function<D, Property_> rightMapping);

    <A, B, C, D, E> PentaJoiner<A, B, C, D, E> newPentaJoiner(PentaPredicate<A, B, C, D, E> filter);

    <A, B, C, D, E, Property_> PentaJoiner<A, B, C, D, E> newPentaJoiner(QuadFunction<A, B, C, D, Property_> leftMapping,
            JoinerType joinerType, Function<E, Property_> rightMapping);

}
