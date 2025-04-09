package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataMultipleSolution.class })
public class MultipleOnlyBaseClassAnnotatedSpringTestConfiguration {

}
