package ai.timefold.solver.spring.boot.autoconfigure.normal;

import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackageClasses = { TestdataSpringEntity.class, TestdataSpringSolution.class })
public class NoConstraintsSpringTestConfiguration {
}
