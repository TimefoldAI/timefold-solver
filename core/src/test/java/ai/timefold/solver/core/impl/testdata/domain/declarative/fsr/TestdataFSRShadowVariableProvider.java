package ai.timefold.solver.core.impl.testdata.domain.declarative.fsr;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableFactory;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableProvider;

import org.jspecify.annotations.NonNull;

public class TestdataFSRShadowVariableProvider implements ShadowVariableProvider {
    public static LocalDateTime BASE_START_TIME = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

    @Override
    public void defineVariables(@NonNull ShadowVariableFactory variableFactory) {
        var previousHandle = variableFactory.entity(TestdataFSRVisit.class).previous();
        var vehicleHandle = variableFactory.entity(TestdataFSRVisit.class).inverse(TestdataFSRVehicle.class);
        var previousFinishTimeHandle = previousHandle.variable("serviceFinishTime", LocalDateTime.class);
        var visitGroupHandle = variableFactory.entity(TestdataFSRVisit.class).group(TestdataFSRVisit::getVisitGroup,
                TestdataFSRVisit.class);
        var visitGroupReadyTimesHandle = visitGroupHandle.intermediates("serviceReadyTime", LocalDateTime.class);

        var serviceReadyTimeHandle = variableFactory.newShadow(TestdataFSRVisit.class)
                .computeIfHas(previousFinishTimeHandle,
                        (visit, previousServiceEndTime) -> previousServiceEndTime.plusMinutes(30L))
                .elseComputeIfHasAll(vehicleHandle,
                        (visit, vehicle) -> BASE_START_TIME)
                .asIntermediate("serviceReadyTime");

        var serviceStartTimeHandle = variableFactory.newShadow(TestdataFSRVisit.class)
                .computeIfHas(visitGroupReadyTimesHandle,
                        (visit, groupReadyTimes) -> groupReadyTimes.isEmpty() ? null : Collections.max(groupReadyTimes))
                .elseComputeIfHasAll(serviceReadyTimeHandle, (visit, readyTime) -> readyTime)
                .as("serviceStartTime");

        var serviceEndTimeHandle = variableFactory.newShadow(TestdataFSRVisit.class)
                .computeIfHas(serviceStartTimeHandle, (visit, startTime) -> startTime.plusMinutes(30))
                .as("serviceFinishTime");
    }
}
