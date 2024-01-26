package ai.timefold.solver.spring.boot.autoconfigure.dummy;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.normal.domain",
        "ai.timefold.solver.spring.boot.autoconfigure.dummy.normal.incrementalScoreConstraints",
        "ai.timefold.solver.spring.boot.autoconfigure.dummy.chained.incrementalScoreConstraints" })
public class MultipleIncrementalScoreConstraintSpringTestConfiguration {
}
