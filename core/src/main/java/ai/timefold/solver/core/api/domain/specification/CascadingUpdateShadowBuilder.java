package ai.timefold.solver.core.api.domain.specification;

import java.util.function.Consumer;

/**
 * Builder for configuring a cascading update shadow variable.
 *
 * @param <S> the solution type
 * @param <E> the entity type
 */
public interface CascadingUpdateShadowBuilder<S, E> {

    CascadingUpdateShadowBuilder<S, E> updateMethod(Consumer<E> updater);

    CascadingUpdateShadowBuilder<S, E> sources(String... sourcePaths);
}
