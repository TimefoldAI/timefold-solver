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
            SelectionOrder resolvedSelectionOrder) {
        switch (resolvedSelectionOrder) {
            case INHERIT:
                throw new IllegalArgumentException("The moveSelectorConfig (" + config
                        + ") has a resolvedSelectionOrder (" + resolvedSelectionOrder
                        + ") which should have been resolved by now.");
            case ORIGINAL:
            case RANDOM:
                break;
            case SORTED:
            case SHUFFLED:
            case PROBABILISTIC:
                if (resolvedCacheType.isNotCached()) {
                    throw new IllegalArgumentException("The moveSelectorConfig (" + config
                            + ") has a resolvedSelectionOrder (" + resolvedSelectionOrder
                            + ") which does not support the resolvedCacheType (" + resolvedCacheType + ").");
                }
                break;
            default:
                throw new IllegalStateException("The resolvedSelectionOrder (" + resolvedSelectionOrder
                        + ") is not implemented.");
        }
    }
}
