package ai.timefold.solver.spring.boot.autoconfigure.dummy;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.basic.domain",
        "ai.timefold.solver.spring.boot.autoconfigure.basic.constraints",
        "ai.timefold.solver.spring.boot.autoconfigure.declarative.constraints" })
public class MultipleConstraintProviderSpringTestConfiguration {
}
