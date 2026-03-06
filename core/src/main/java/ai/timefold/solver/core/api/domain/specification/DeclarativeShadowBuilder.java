package ai.timefold.solver.core.api.domain.specification;

import java.util.function.Function;

/**
 * Builder for configuring a declarative shadow variable.
 *
 * @param <S> the solution type
 * @param <E> the entity type
 * @param <V> the shadow variable value type
 */
public interface DeclarativeShadowBuilder<S, E, V> {

    DeclarativeShadowBuilder<S, E, V> supplier(Function<E, V> supplier);

    DeclarativeShadowBuilder<S, E, V> sources(String... sourcePaths);

    DeclarativeShadowBuilder<S, E, V> alignmentKey(String key);
}
