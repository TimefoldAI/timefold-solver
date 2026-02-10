package ai.timefold.solver.spring.boot.autoconfigure.dummy;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.dummy.basic.noSolution",
        "ai.timefold.solver.spring.boot.autoconfigure.dummy.basic.constraints.incremental" })
public class NoSolutionSpringTestConfiguration {
}
