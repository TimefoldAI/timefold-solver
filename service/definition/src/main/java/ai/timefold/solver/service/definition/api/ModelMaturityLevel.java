package ai.timefold.solver.service.definition.api;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum ModelMaturityLevel {

    @JsonAlias("Example") // To ensure backward compatibility with already built models.
    Template,
    Experimental,
    Preview,
    Stable,
    Deprecated
}
