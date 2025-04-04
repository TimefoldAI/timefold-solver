package ai.timefold.solver.spring.boot.autoconfigure.superclass;

import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestMultipleConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackageClasses = { TestdataMultipleSolution.class, TestdataMultipleChildEntity.class,
        TestMultipleConstraintProvider.class })
public class MultipleBothClassesAnnotatedBaseInterfaceSpringTestConfiguration {

}
