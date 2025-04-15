package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.childnot.TestdataChildNotAnnotatedInterfaceSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataChildNotAnnotatedInterfaceSolution.class })
public class OnlyBaseAnnotatedInterfaceSpringTestConfiguration {

}
