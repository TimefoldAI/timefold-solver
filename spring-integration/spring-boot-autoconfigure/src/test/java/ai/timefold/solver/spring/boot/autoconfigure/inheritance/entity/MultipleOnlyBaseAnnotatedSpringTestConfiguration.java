package ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity;

import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childnot.TestdataMultipleChildNotAnnotatedSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataMultipleChildNotAnnotatedSolution.class })
public class MultipleOnlyBaseAnnotatedSpringTestConfiguration {

}
