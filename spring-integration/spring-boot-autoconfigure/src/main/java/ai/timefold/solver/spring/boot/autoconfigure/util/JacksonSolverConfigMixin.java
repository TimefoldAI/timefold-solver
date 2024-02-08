package ai.timefold.solver.spring.boot.autoconfigure.util;

import java.util.Map;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class JacksonSolverConfigMixin {
    @JsonIgnore
    public abstract ClassLoader getClassLoader();

    @JsonIgnore
    public abstract Map<String, MemberAccessor> getGizmoMemberAccessorMap();

    @JsonIgnore
    public abstract Map<String, SolutionCloner> getGizmoSolutionClonerMap();
}
