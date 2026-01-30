package ai.timefold.solver.spring.boot.autoconfigure.dummy;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.normal.domain",
        "ai.timefold.solver.spring.boot.autoconfigure.chained.domain" })
public class MultipleSolutionsSpringTestConfiguration {
}
