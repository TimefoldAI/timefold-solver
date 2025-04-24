package ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.replacevar.TestdataReplaceVarSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataReplaceVarSolution.class, DummyConstraintProvider.class })
public class ReplaceVarSpringTestConfiguration {

}
