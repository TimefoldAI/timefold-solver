package ai.timefold.solver.spring.boot.autoconfigure.declarative;

import ai.timefold.solver.spring.boot.autoconfigure.declarative.constraints.TestdataSpringSupplierVariableConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.domain.TestdataSpringSupplierVariableSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackageClasses = { TestdataSpringSupplierVariableSolution.class,
        TestdataSpringSupplierVariableConstraintProvider.class })
@AutoConfigurationPackage
public class SupplierVariableSpringTestConfiguration {

}
