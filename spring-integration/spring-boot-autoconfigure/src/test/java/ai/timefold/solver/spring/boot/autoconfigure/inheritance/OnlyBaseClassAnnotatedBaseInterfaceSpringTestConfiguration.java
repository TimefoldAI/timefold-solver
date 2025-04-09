package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childnotannotated.TestdataSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataSolution.class })
public class OnlyBaseClassAnnotatedBaseInterfaceSpringTestConfiguration {

}
