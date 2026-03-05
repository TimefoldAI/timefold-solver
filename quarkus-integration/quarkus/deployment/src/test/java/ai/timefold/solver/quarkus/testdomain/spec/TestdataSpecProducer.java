package ai.timefold.solver.quarkus.testdomain.spec;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.score.SimpleScore;

@ApplicationScoped
public class TestdataSpecProducer {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Produces
    @Singleton
    PlanningSpecification planningSpec() {
        return PlanningSpecification.of(TestdataSpecSolution.class)
                .score(SimpleScore.class, TestdataSpecSolution::getScore, TestdataSpecSolution::setScore)
                .problemFacts("valueList", TestdataSpecSolution::getValueList,
                        (solution, value) -> solution.setValueList((java.util.List<String>) value))
                .entityCollection("entityList", TestdataSpecSolution::getEntityList,
                        (solution, value) -> solution.setEntityList(
                                (java.util.List<TestdataSpecEntity>) value))
                .valueRange("valueRange", TestdataSpecSolution::getValueList)
                .entity(TestdataSpecEntity.class, e -> e
                        .variable("value", String.class, v -> v
                                .accessors(TestdataSpecEntity::getValue, TestdataSpecEntity::setValue)
                                .valueRange("valueRange")))
                .cloning(c -> c
                        .solutionFactory(TestdataSpecSolution::new)
                        .entityFactory(TestdataSpecEntity.class, TestdataSpecEntity::new))
                .build();
    }

}
