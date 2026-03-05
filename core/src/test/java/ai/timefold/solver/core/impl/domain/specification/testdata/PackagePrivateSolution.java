package ai.timefold.solver.core.impl.domain.specification.testdata;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;

/**
 * A planning solution with package-private visibility for testing Lookup-based access.
 */
@PlanningSolution
class PackagePrivateSolution {

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<PackagePrivateValue> values;

    @PlanningEntityCollectionProperty
    private List<PackagePrivateEntity> entities;

    @PlanningScore
    private SimpleScore score;

    PackagePrivateSolution() {
    }

    PackagePrivateSolution(List<PackagePrivateValue> values, List<PackagePrivateEntity> entities) {
        this.values = values;
        this.entities = entities;
    }

    List<PackagePrivateValue> getValues() {
        return values;
    }

    void setValues(List<PackagePrivateValue> values) {
        this.values = values;
    }

    List<PackagePrivateEntity> getEntities() {
        return entities;
    }

    void setEntities(List<PackagePrivateEntity> entities) {
        this.entities = entities;
    }

    SimpleScore getScore() {
        return score;
    }

    void setScore(SimpleScore score) {
        this.score = score;
    }
}
