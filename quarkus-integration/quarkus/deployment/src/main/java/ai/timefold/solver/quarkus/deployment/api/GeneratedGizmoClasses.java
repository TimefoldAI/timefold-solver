package ai.timefold.solver.quarkus.deployment.api;

import java.util.Set;

public record GeneratedGizmoClasses(Set<String> memberAccessorClassSet) {

    public GeneratedGizmoClasses {
        memberAccessorClassSet = Set.copyOf(memberAccessorClassSet);
    }

}
