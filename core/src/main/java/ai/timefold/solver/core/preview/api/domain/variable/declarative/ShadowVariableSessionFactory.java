package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.MockShadowVariableSessionFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ShadowVariableSessionFactory {
    static ShadowVariableSessionFactory create(SolutionDescriptor<?> solutionDescriptor,
            ShadowVariableProvider shadowVariableProvider) {
        return new MockShadowVariableSessionFactory<>(solutionDescriptor, shadowVariableProvider);
    }

    ShadowVariableSession forEntities(Object... entities);
}
