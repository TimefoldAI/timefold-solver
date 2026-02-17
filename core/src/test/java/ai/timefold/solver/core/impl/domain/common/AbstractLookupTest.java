package ai.timefold.solver.core.impl.domain.common;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;

import org.junit.jupiter.api.BeforeEach;

abstract class AbstractLookupTest {

    private final LookUpStrategyType lookUpStrategyType;
    protected LookupManager lookUpManager;

    protected AbstractLookupTest(LookUpStrategyType lookUpStrategyType) {
        this.lookUpStrategyType = lookUpStrategyType;
    }

    @BeforeEach
    void setUpLookUpManager() {
        lookUpManager = new LookupManager(createLookupStrategyResolver(lookUpStrategyType));
    }

    protected LookupStrategyResolver createLookupStrategyResolver(LookUpStrategyType lookUpStrategyType) {
        DescriptorPolicy descriptorPolicy = new DescriptorPolicy();
        descriptorPolicy.setMemberAccessorFactory(new MemberAccessorFactory());
        descriptorPolicy.setDomainAccessType(DomainAccessType.FORCE_REFLECTION);
        return new LookupStrategyResolver(descriptorPolicy, lookUpStrategyType);
    }
}
