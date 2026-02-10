package ai.timefold.solver.spring.boot.autoconfigure.dummy;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackages = { "ai.timefold.solver.spring.boot.autoconfigure.basic.domain",
        "ai.timefold.solver.spring.boot.autoconfigure.gizmo.domain" })
public class MultipleSolutionsSpringTestConfiguration {
}
