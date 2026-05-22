package ai.timefold.solver.model.maps.service.client.util;

import java.util.List;
import java.util.Optional;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.model.definition.api.Status;
import ai.timefold.solver.model.definition.api.UserModel;
import ai.timefold.solver.model.definition.api.metrics.InputMetricsAware;
import ai.timefold.solver.model.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.model.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.model.definition.api.metrics.OutputMetricsAware;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.service.integration.api.LocationsAwareSolverModel;

@PlanningSolution
public class SampleModel implements LocationsAwareSolverModel<HardSoftScore>, InputMetricsAware<ModelInputMetrics>,
        OutputMetricsAware<ModelOutputMetrics>,
        UserModel<HardSoftScore, Status<HardSoftScore>> {

    private final String locationSetName;
    private final List<Location> locations;
    private List<Location> locationsNotInMap;

    private List<SampleEntity> entityList;
    private List<String> valueList;
    private HardSoftScore score;
    private ConstraintWeightOverrides<HardSoftScore> constraintWeightOverrides;

    // required by solver
    public SampleModel() {
        this(List.of());
    }

    public SampleModel(List<Location> locations) {
        this.locations = locations;
        this.locationSetName = null;
    }

    public SampleModel(String locationSetName, List<Location> locations) {
        this.locations = locations;
        this.locationSetName = locationSetName;
    }

    @PlanningEntityCollectionProperty
    public List<SampleEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<SampleEntity> entityList) {
        this.entityList = entityList;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    @PlanningScore
    @Override
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    @Override
    public ConstraintWeightOverrides<HardSoftScore> getConstraintWeightOverrides() {
        return this.constraintWeightOverrides;
    }

    public void setConstraintWeightOverrides(ConstraintWeightOverrides<HardSoftScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }

    @Override
    public ModelInputMetrics getInputMetrics() {
        return null;
    }

    @Override
    public ModelOutputMetrics getOutputMetrics() {
        return null;
    }

    @Override
    public Status<HardSoftScore> getStatus() {
        return null;
    }

    @Override
    public Status<HardSoftScore> newStatus(String name) {
        return null;
    }

    @Override
    public List<Location> getLocations() {
        return this.locations;
    }

    @Override
    public Optional<String> getLocationSetName() {
        return Optional.ofNullable(locationSetName);
    }

    @Override
    public void setLocationsNotInMap(List<Location> locationsNotInMap) {
        this.locationsNotInMap = locationsNotInMap;
    }

    @Override
    public List<Location> getLocationsNotInMap() {
        return locationsNotInMap;
    }

    @Override
    public void setStatus(Status<HardSoftScore> status) {
        // Noop
    }
}
