package ai.timefold.solver.spring.boot.autoconfigure.util;

import java.util.List;

import ai.timefold.solver.core.impl.phase.custom.CustomPhaseCommand;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class JacksonCustomPhaseConfigMixin {
    @JsonIgnore
    public abstract List<CustomPhaseCommand> getCustomPhaseCommandList();
}
