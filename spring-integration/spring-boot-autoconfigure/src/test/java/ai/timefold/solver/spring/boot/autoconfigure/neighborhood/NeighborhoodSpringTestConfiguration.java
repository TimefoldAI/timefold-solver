package ai.timefold.solver.spring.boot.autoconfigure.neighborhood;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.basic.domain",
        "ai.timefold.solver.spring.boot.autoconfigure.basic.constraints",
        "ai.timefold.solver.spring.boot.autoconfigure.neighborhood.single" })
public class NeighborhoodSpringTestConfiguration {
}
