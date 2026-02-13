package ai.timefold.solver.core.impl.domain.lookup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.solution.cloner.DeepCloningUtils;

import org.jspecify.annotations.NullMarked;

/**
 * This class is thread-safe.
 */
@NullMarked
public final class LookUpStrategyResolver {

    private final LookUpStrategyType lookUpStrategyType;
    private final DomainAccessType domainAccessType;
    private final MemberAccessorFactory memberAccessorFactory;
    private final Map<Class<?>, LookUpStrategy> decisionCache = new ConcurrentHashMap<>();

    public LookUpStrategyResolver(DescriptorPolicy descriptorPolicy) {
        this(descriptorPolicy, LookUpStrategyType.PLANNING_ID_OR_NONE);
    }

    LookUpStrategyResolver(DescriptorPolicy descriptorPolicy, LookUpStrategyType lookUpStrategyType) {
        this.lookUpStrategyType = lookUpStrategyType;
        this.domainAccessType = descriptorPolicy.getDomainAccessType();
        this.memberAccessorFactory = descriptorPolicy.getMemberAccessorFactory();
    }

    /**
     * This method is thread-safe,
     * in a sense that it can be called by multiple threads at the same time,
     * and it will always return the same result for the same input.
     *
     * @param object never null
     * @return never null
     */
    public LookUpStrategy determineLookUpStrategy(Object object) {
        var objectClass = object.getClass();
        var decision = decisionCache.get(objectClass);
        if (decision == null) { // Simulate computeIfAbsent, avoiding creating a lambda on the hot path.
            if (DeepCloningUtils.isImmutable(objectClass)) {
                decision = new ImmutableLookUpStrategy();
            } else {
                decision = switch (lookUpStrategyType) {
                    case NONE -> new NoneLookUpStrategy();
                    case PLANNING_ID_OR_NONE -> {
                        var memberAccessor =
                                ConfigUtils.findPlanningIdMemberAccessor(objectClass, memberAccessorFactory, domainAccessType);
                        if (memberAccessor == null) {
                            yield new NoneLookUpStrategy();
                        }
                        yield new PlanningIdLookUpStrategy(memberAccessor);
                    }
                    case PLANNING_ID_OR_FAIL_FAST -> {
                        var memberAccessor =
                                ConfigUtils.findPlanningIdMemberAccessor(objectClass, memberAccessorFactory, domainAccessType);
                        if (memberAccessor == null) {
                            throw new IllegalArgumentException("""
                                    The class (%s) does not have a @%s annotation, but the lookUpStrategyType (%s) requires it.
                                    Maybe add a @%s annotation?"""
                                    .formatted(objectClass, PlanningId.class.getSimpleName(), lookUpStrategyType,
                                            PlanningId.class.getSimpleName()));
                        }
                        yield new PlanningIdLookUpStrategy(memberAccessor);
                    }
                };
                decisionCache.put(objectClass, decision);
            }
        }
        return decision;
    }

}
