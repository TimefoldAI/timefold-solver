package ai.timefold.solver.core.preview.api.variable.provided;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

public class TestShadowVariableProvider implements ShadowVariableProvider {
    static LocalDateTime BASE_START_TIME = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

    @Override
    public void defineVariables(ShadowVariableFactory variableFactory) {
        var serviceReadyTime = variableFactory.newShadow(Visit.class)
                .compute(variableFactory.entity(Visit.class).previous()
                        .variable(LocalDateTime.class, "serviceFinishTime"),
                        (visit, previousServiceEndTime) -> previousServiceEndTime.plusMinutes(30L))
                .orCompute(variableFactory.entity(Visit.class).inverse(Vehicle.class),
                        (visit, vehicle) -> BASE_START_TIME)
                .as("serviceReadyTime");

        var serviceStartTime = variableFactory.newShadow(Visit.class)
                .compute(variableFactory
                        .entity(Visit.class)
                        .group(Visit.class, Visit::getVisitGroup)
                        .variables(LocalDateTime.class, "serviceReadyTime"),
                        (visit, groupReadyTimes) -> groupReadyTimes.isEmpty() ? null : Collections.max(groupReadyTimes))
                .orCompute(serviceReadyTime, (visit, readyTime) -> readyTime)
                .as("serviceStartTime");

        var serviceEndTime = variableFactory.newShadow(Visit.class)
                .compute(serviceStartTime, (visit, startTime) -> startTime.plusMinutes(30))
                .as("serviceFinishTime");
    }
}
