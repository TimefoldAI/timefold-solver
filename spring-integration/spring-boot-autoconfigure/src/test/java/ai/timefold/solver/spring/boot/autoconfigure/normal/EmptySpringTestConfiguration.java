package ai.timefold.solver.spring.boot.autoconfigure.normal;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackages = "ai.timefold.solver.spring.boot.autoconfigure.empty")
public class EmptySpringTestConfiguration {
}
