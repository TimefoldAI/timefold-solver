package ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity;

import ai.timefold.solver.core.testconstraint.DummyConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.replacevar.TestdataReplaceVarInterfaceSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataReplaceVarInterfaceSolution.class, DummyConstraintProvider.class })
public class ReplaceVarInterfaceSpringTestConfiguration {

}
