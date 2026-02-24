package ai.timefold.solver.spring.boot.it.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = IntegrationTestValue.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public record IntegrationTestValue(String id) {
}
