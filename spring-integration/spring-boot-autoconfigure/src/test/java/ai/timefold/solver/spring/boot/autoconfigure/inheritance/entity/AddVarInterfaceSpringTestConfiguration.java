package ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity;

import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.addvar.TestdataAddVarInterfaceSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataAddVarInterfaceSolution.class })
public class AddVarInterfaceSpringTestConfiguration {

}
