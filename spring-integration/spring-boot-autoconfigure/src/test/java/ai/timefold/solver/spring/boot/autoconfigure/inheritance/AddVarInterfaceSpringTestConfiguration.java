package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.addvar.TestdataAddVarInterfaceSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataAddVarInterfaceSolution.class })
public class AddVarInterfaceSpringTestConfiguration {

}
