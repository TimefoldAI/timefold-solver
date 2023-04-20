package ai.timefold.solver.core.impl.domain.lookup;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;

import org.junit.jupiter.api.BeforeEach;

abstract class AbstractLookupTest {

    private final LookUpStrategyType lookUpStrategyType;
    protected LookUpManager lookUpManager;

    protected AbstractLookupTest(LookUpStrategyType lookUpStrategyType) {
        this.lookUpStrategyType = lookUpStrategyType;
    }

    @BeforeEach
    void setUpLookUpManager() {
        lookUpManager = new LookUpManager(createLookupStrategyResolver(lookUpStrategyType));
    }

    protected LookUpStrategyResolver createLookupStrategyResolver(LookUpStrategyType lookUpStrategyType) {
        DescriptorPolicy descriptorPolicy = new DescriptorPolicy();
        descriptorPolicy.setMemberAccessorFactory(new MemberAccessorFactory());
        descriptorPolicy.setDomainAccessType(DomainAccessType.REFLECTION);
        return new LookUpStrategyResolver(descriptorPolicy, lookUpStrategyType);
    }
}
