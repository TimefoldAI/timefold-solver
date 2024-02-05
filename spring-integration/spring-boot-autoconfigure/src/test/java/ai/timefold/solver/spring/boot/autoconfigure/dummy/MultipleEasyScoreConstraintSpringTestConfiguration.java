package ai.timefold.solver.spring.boot.autoconfigure.dummy;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.normal.domain",
        "ai.timefold.solver.spring.boot.autoconfigure.dummy.normal.constraints.easy",
        "ai.timefold.solver.spring.boot.autoconfigure.dummy.chained.constraints.easy" })
public class MultipleEasyScoreConstraintSpringTestConfiguration {
}
