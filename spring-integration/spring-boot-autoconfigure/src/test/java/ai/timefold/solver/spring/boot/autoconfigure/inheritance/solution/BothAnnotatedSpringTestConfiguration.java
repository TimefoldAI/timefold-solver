package ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackageClasses = { TestdataBothAnnotatedExtendedSolution.class })
public class BothAnnotatedSpringTestConfiguration {

}
