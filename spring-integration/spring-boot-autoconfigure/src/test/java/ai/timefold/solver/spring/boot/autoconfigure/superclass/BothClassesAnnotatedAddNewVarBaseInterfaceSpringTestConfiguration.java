package ai.timefold.solver.spring.boot.autoconfigure.superclass;

import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotatedaddvar.TestConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotatedaddvar.TestdataChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotatedaddvar.TestdataSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackageClasses = { TestdataSolution.class, TestdataChildEntity.class, TestConstraintProvider.class })
public class BothClassesAnnotatedAddNewVarBaseInterfaceSpringTestConfiguration {

}
