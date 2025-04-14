package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.replacevar.TestdataReplaceVarInterfaceSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataReplaceVarInterfaceSolution.class, DummyConstraintProvider.class })
public class ReplaceVarInterfaceSpringTestConfiguration {

}
