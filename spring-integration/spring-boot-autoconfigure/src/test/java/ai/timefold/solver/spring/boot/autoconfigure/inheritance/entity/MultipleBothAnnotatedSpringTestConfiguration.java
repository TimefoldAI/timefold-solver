package ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.multiple.baseannotated.classes.childtoo.TestdataMultipleBothAnnotatedSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage(basePackageClasses = { TestdataMultipleBothAnnotatedSolution.class })
public class MultipleBothAnnotatedSpringTestConfiguration {

}
