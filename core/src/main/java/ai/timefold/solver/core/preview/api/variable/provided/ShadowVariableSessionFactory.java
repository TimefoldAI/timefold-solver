package ai.timefold.solver.core.preview.api.variable.provided;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.provided.MockShadowVariableSessionFactory;

public interface ShadowVariableSessionFactory {
    static ShadowVariableSessionFactory create(SolutionDescriptor<?> solutionDescriptor,
            ShadowVariableProvider shadowVariableProvider) {
        return new MockShadowVariableSessionFactory<>(solutionDescriptor, shadowVariableProvider);
    }

    ShadowVariableSession forEntities(Object... entities);
}
