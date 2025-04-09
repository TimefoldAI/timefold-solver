package ai.timefold.solver.spring.boot.autoconfigure.dummy;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.dummy.normal.noEntity",
        "ai.timefold.solver.spring.boot.autoconfigure.dummy.normal.constraints.incremental" })
public class NoEntitySpringTestConfiguration {
}
