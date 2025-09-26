package ai.timefold.solver.core.impl.heuristic.selector;

import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;

public abstract class AbstractSelectorFactory<Solution_, SelectorConfig_ extends SelectorConfig<SelectorConfig_>>
        extends AbstractFromConfigFactory<Solution_, SelectorConfig_> {

    protected AbstractSelectorFactory(SelectorConfig_ selectorConfig) {
        super(selectorConfig);
    }

    protected void validateCacheTypeVersusSelectionOrder(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, boolean hasEntityRange) {
        switch (resolvedSelectionOrder) {
            case INHERIT:
                throw new IllegalArgumentException(
                        "The moveSelectorConfig (%s) has a resolvedSelectionOrder (%s) which should have been resolved by now."
                                .formatted(config, resolvedSelectionOrder));
            case ORIGINAL, RANDOM:
                break;
            case SORTED: {
                if (resolvedCacheType.isNotCached()) {
                    throw new IllegalArgumentException(
                            "The moveSelectorConfig (%s) has a resolvedSelectionOrder (%s) which does not support the resolvedCacheType (%s)."
                                    .formatted(config, resolvedSelectionOrder, resolvedCacheType));
                }
                if (hasEntityRange && resolvedCacheType != SelectionCacheType.STEP) {
                    throw new IllegalArgumentException(
                            """
                                    The moveSelectorConfig (%s) has a resolvedSelectionOrder (%s) which does not support the resolvedCacheType (%s).
                                    Maybe set the "cacheType" to STEP."""
                                    .formatted(config, resolvedSelectionOrder, resolvedCacheType));
                }
                break;
            }
            case SHUFFLED, PROBABILISTIC:
                if (resolvedCacheType.isNotCached()) {
                    throw new IllegalArgumentException(
                            "The moveSelectorConfig (%s) has a resolvedSelectionOrder (%s) which does not support the resolvedCacheType (%s)."
                                    .formatted(config, resolvedSelectionOrder, resolvedCacheType));
                }
                break;
            default:
                throw new IllegalStateException(
                        "The resolvedSelectionOrder (%s) is not implemented.".formatted(resolvedSelectionOrder));
        }
    }
}
