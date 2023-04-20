package ai.timefold.solver.spring.boot.autoconfigure.chained;

import ai.timefold.solver.spring.boot.autoconfigure.chained.constraints.TestdataChainedSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackageClasses = { TestdataChainedSpringSolution.class, TestdataChainedSpringConstraintProvider.class })
@AutoConfigurationPackage
public class ChainedSpringTestConfiguration {

}
