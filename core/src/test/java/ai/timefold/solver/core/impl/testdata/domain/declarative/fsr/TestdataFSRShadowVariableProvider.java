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
        var serviceReadyTime = variableFactory.newShadow(TestdataFSRVisit.class)
                .computeIfHasAll(variableFactory.entity(TestdataFSRVisit.class).previous()
                        .variable(LocalDateTime.class, "serviceFinishTime"),
                        (visit, previousServiceEndTime) -> previousServiceEndTime.plusMinutes(30L))
                .elseComputeIfHasAll(variableFactory.entity(TestdataFSRVisit.class).inverse(TestdataFSRVehicle.class),
                        (visit, vehicle) -> BASE_START_TIME)
                .as("serviceReadyTime");

        var serviceStartTime = variableFactory.newShadow(TestdataFSRVisit.class)
                .computeIfHasAll(variableFactory
                        .entity(TestdataFSRVisit.class)
                        .group(TestdataFSRVisit.class, TestdataFSRVisit::getVisitGroup)
                        .variables(LocalDateTime.class, "serviceReadyTime"),
                        (visit, groupReadyTimes) -> groupReadyTimes.isEmpty() ? null : Collections.max(groupReadyTimes))
                .elseComputeIfHasAll(serviceReadyTime, (visit, readyTime) -> readyTime)
                .as("serviceStartTime");

        var serviceEndTime = variableFactory.newShadow(TestdataFSRVisit.class)
                .computeIfHasAll(serviceStartTime, (visit, startTime) -> startTime.plusMinutes(30))
                .as("serviceFinishTime");
    }
}
