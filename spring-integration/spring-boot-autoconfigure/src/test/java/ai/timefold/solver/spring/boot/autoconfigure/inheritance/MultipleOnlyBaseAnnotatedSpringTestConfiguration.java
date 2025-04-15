package ai.timefold.solver.spring.boot.autoconfigure.inheritance;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.classes.childnot.TestdataMultipleChildNotAnnotatedSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataMultipleChildNotAnnotatedSolution.class })
public class MultipleOnlyBaseAnnotatedSpringTestConfiguration {

}
