package ai.timefold.solver.core.impl.testdata.domain.fsr;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableFactory;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableProvider;

public class TestShadowVariableProvider implements ShadowVariableProvider {
    public static LocalDateTime BASE_START_TIME = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

    @Override
    public void defineVariables(ShadowVariableFactory variableFactory) {
        var serviceReadyTime = variableFactory.newShadow(TestdataFSRVisit.class)
                .compute(variableFactory.entity(TestdataFSRVisit.class).previous()
                        .variable(LocalDateTime.class, "serviceFinishTime"),
                        (visit, previousServiceEndTime) -> previousServiceEndTime.plusMinutes(30L))
                .orCompute(variableFactory.entity(TestdataFSRVisit.class).inverse(TestdataFSRVehicle.class),
                        (visit, vehicle) -> BASE_START_TIME)
                .as("serviceReadyTime");

        var serviceStartTime = variableFactory.newShadow(TestdataFSRVisit.class)
                .compute(variableFactory
                        .entity(TestdataFSRVisit.class)
                        .group(TestdataFSRVisit.class, TestdataFSRVisit::getVisitGroup)
                        .variables(LocalDateTime.class, "serviceReadyTime"),
                        (visit, groupReadyTimes) -> groupReadyTimes.isEmpty() ? null : Collections.max(groupReadyTimes))
                .orCompute(serviceReadyTime, (visit, readyTime) -> readyTime)
                .as("serviceStartTime");

        var serviceEndTime = variableFactory.newShadow(TestdataFSRVisit.class)
                .compute(serviceStartTime, (visit, startTime) -> startTime.plusMinutes(30))
                .as("serviceFinishTime");
    }
}
