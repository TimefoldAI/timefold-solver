package ai.timefold.solver.spring.boot.autoconfigure.superclass;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.basenotannotated.TestdataChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.basenotannotated.TestdataSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackageClasses = { TestdataSolution.class, TestdataChildEntity.class, DummyConstraintProvider.class })
public class OnlyChildClassAnnotatedBaseInterfaceSpringTestConfiguration {

}
