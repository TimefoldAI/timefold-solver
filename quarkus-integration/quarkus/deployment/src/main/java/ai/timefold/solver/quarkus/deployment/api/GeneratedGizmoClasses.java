package ai.timefold.solver.quarkus.deployment.api;

import java.util.Set;

public record GeneratedGizmoClasses(Set<String> memberAccessorClassSet, Set<String> solutionClonerClassSet) {

    public GeneratedGizmoClasses {
        memberAccessorClassSet = Set.copyOf(memberAccessorClassSet);
        solutionClonerClassSet = Set.copyOf(solutionClonerClassSet);
    }

}
