package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.classes.childtoo.TestdataBothAnnotatedSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackageClasses = { TestdataBothAnnotatedSolution.class })
public class BothAnnotatedSpringTestConfiguration {

}
