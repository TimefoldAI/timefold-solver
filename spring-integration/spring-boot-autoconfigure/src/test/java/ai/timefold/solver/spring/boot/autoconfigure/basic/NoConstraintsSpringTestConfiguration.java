package ai.timefold.solver.spring.boot.autoconfigure.basic;

import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackageClasses = { TestdataSpringEntity.class, TestdataSpringSolution.class })
public class NoConstraintsSpringTestConfiguration {
}
