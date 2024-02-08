package ai.timefold.solver.spring.boot.autoconfigure.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class JacksonTerminationConfigMixin {
    @JsonIgnore
    public abstract boolean isConfigured();
}
