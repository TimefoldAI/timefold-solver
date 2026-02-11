package ai.timefold.solver.spring.boot.autoconfigure.multimodule;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackages = "ai.timefold.solver.spring.boot.autoconfigure.basic")
public class MultiModuleSpringTestConfiguration {

}
