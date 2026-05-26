package ai.timefold.solver.tools.maven.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public record ConfigurationInfo(String containerRegistry) {

}
