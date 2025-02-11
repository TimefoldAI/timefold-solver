package ai.timefold.solver.core.preview.api.variable.provided;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.provided.DefaultShadowVariableSessionFactory;

public interface ShadowVariableSessionFactory {
    static ShadowVariableSessionFactory create(SolutionDescriptor<?> solutionDescriptor,
            ShadowVariableProvider shadowVariableProvider) {
        return new DefaultShadowVariableSessionFactory<>(solutionDescriptor, shadowVariableProvider);
    }

    ShadowVariableSession forEntities(Object... entities);
}
