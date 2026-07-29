package ai.timefold.solver.service.testmodel;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.SolvingStatus;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.domain.ModelRequest;
import ai.timefold.solver.service.definition.api.domain.ModelResponse;
import ai.timefold.solver.service.definition.api.rest.OperationOnPost;
import ai.timefold.solver.service.definition.api.validation.IssueCode;
import ai.timefold.solver.service.definition.api.validation.IssueSeverity;
import ai.timefold.solver.service.definition.api.validation.IssueType;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationIssueTypes;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;
import ai.timefold.solver.service.definition.internal.events.BestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.DatasetComputedEvent;
import ai.timefold.solver.service.definition.internal.events.FinalBestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.SolverChannels;
import ai.timefold.solver.service.quarkus.deployment.defaults.EmptyModelConfigOverrides;
import ai.timefold.solver.service.testmodel.domain.Employee;
import ai.timefold.solver.service.testmodel.domain.EmployeeSchedule;
import ai.timefold.solver.service.testmodel.domain.Shift;
import ai.timefold.solver.service.testmodel.domain.Skill;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.client.SseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;

@QuarkusTest
@QuarkusTestResource(InMemoryMessagingTestResource.class)
public class EmployeeScheduleResourceTest {

    private static final Duration TEST_AWAIT_TIMEOUT_DURATION = Duration.ofSeconds(60);
    private static final Duration TEST_POLL_INTERVAL_MILLIS = Duration.ofMillis(500);

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    private static final OffsetDateTime TOMORROW_08_00 = OffsetDateTime.of(TOMORROW, LocalTime.of(8, 0), ZoneOffset.UTC);
    private static final OffsetDateTime TOMORROW_16_00 = OffsetDateTime.of(TOMORROW, LocalTime.of(16, 0), ZoneOffset.UTC);

    @Inject
    @Connector("smallrye-in-memory")
    InMemoryConnector connector;

    InMemorySink<InitSolutionEvent> initSolutionSink;
    InMemorySink<BestSolutionEvent> bestSolutionSink;
    InMemorySink<FinalBestSolutionEvent> finalBestSolutionSink;
    InMemorySink<DatasetComputedEvent> datasetComputedSink;

    @ConfigProperty(name = "model.api.version")
    String modelApiVersion;

    @TestHTTPResource
    URI baseUri;

    interface SseClient {
        @GET
        @Path("/{apiversion}/schedules/{id}/events")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        Multi<SseEvent<Metadata<HardMediumSoftScore>>> getEvents(@PathParam("apiversion") String apiversion,
                @PathParam("id") String id);
    }

    @BeforeEach
    void before() {
        RestAssured.basePath = "/" + modelApiVersion;
        datasetComputedSink = connector.sink(SolverChannels.DATASET_COMPUTED);
        datasetComputedSink.clear();
        initSolutionSink = connector.sink(SolverChannels.INIT_SOLUTION);
        initSolutionSink.clear();
        bestSolutionSink = connector.sink(SolverChannels.BEST_SOLUTION);
        bestSolutionSink.clear();
        finalBestSolutionSink = connector.sink(SolverChannels.FINAL_BEST_SOLUTION);
        finalBestSolutionSink.clear();
    }

    @Test
    void constraintWeightsAvailableAfterDatasetComputed() {
        EmployeeSchedule inputSchedule = createInputEmployeeSchedule();

        post(inputSchedule);

        await()
                .atMost(TEST_AWAIT_TIMEOUT_DURATION)
                .pollInterval(TEST_POLL_INTERVAL_MILLIS)
                .until(() -> {
                    return !datasetComputedSink.received().isEmpty();
                });
        // model extracted form the event payload
        SolverModel model = datasetComputedSink.received().getFirst().getPayload().getModel();

        assertThat(model.getScore()).isNotNull();
        assertThat(model.getConstraintWeightOverrides()).isNotNull();
    }

    static Stream<Arguments> solveRequestedScenarios() {
        return Stream.of(
                Arguments.of(OperationOnPost.SOLVE, true),
                Arguments.of(OperationOnPost.NONE, false));
    }

    @ParameterizedTest
    @MethodSource("solveRequestedScenarios")
    void datasetComputedEventCarriesSolveRequestedFlag(OperationOnPost operation, boolean expectedSolveRequested) {
        EmployeeSchedule inputSchedule = createInputEmployeeSchedule();

        post(inputSchedule, operation);

        await()
                .atMost(TEST_AWAIT_TIMEOUT_DURATION)
                .pollInterval(TEST_POLL_INTERVAL_MILLIS)
                .until(() -> !datasetComputedSink.received().isEmpty());

        DatasetComputedEvent event = datasetComputedSink.received().getFirst().getPayload();
        assertThat(event.isSolveRequested()).isEqualTo(expectedSolveRequested);
    }

    @Test
    void solveEmployeeSchedule() {
        EmployeeSchedule inputSchedule = createInputEmployeeSchedule();

        Metadata<HardMediumSoftScore> metadata = post(inputSchedule);
        EmployeeSchedule outputSchedule = awaitFeasiblyAssigned(metadata);

        assertAllShiftsAssigned(outputSchedule);

        assertThat(initSolutionSink.received()).hasSize(1);
        assertThat(initSolutionSink.received().getFirst().getPayload().getEventProducerId())
                .isEqualTo("Construction Heuristic (0)");
        assertThat(bestSolutionSink.received()).hasSize(2);
        assertThat(bestSolutionSink.received().getFirst().getPayload().getEventProducerId())
                .isEqualTo("Solving started");
        assertThat(bestSolutionSink.received().getLast().getPayload().getEventProducerId())
                .isEqualTo("Construction Heuristic (0)");
        assertThat(finalBestSolutionSink.received()).hasSize(1);
        assertThat(finalBestSolutionSink.received().getFirst().getPayload().getEventProducerId()).isNull();
    }

    @Test
    void uploadAndSolveEmployeeSchedule() {
        EmployeeSchedule inputSchedule = createInputEmployeeSchedule();

        Metadata<HardMediumSoftScore> uploadMetadata = post(inputSchedule, OperationOnPost.NONE);
        assertThat(uploadMetadata.getSolverStatus()).isIn(SolvingStatus.DATASET_CREATED,
                SolvingStatus.DATASET_VALIDATED,
                SolvingStatus.DATASET_COMPUTED);

        Metadata<HardMediumSoftScore> solveMetadata = post(uploadMetadata);
        EmployeeSchedule outputSchedule = awaitFeasiblyAssigned(solveMetadata);

        assertAllShiftsAssigned(outputSchedule);
    }

    @Test
    void uploadOnlySchedule() {
        EmployeeSchedule inputSchedule = createInputEmployeeSchedule();

        Metadata<HardMediumSoftScore> uploadMetadata = post(inputSchedule, OperationOnPost.NONE);

        ModelResponse<HardMediumSoftScore, EmployeeSchedule, EmployeeScheduleInputMetrics, EmployeeScheduleOutputMetrics> response =
                getModelResponse(uploadMetadata);

        assertThat(response.inputMetrics().employeeCount()).isEqualTo(3);
        assertThat(response.inputMetrics().shiftCount()).isEqualTo(4);
        assertThat(response.outputMetrics().assignedShifts()).isZero();
    }

    @Test
    void solveEmployeeScheduleWithWatching() throws InterruptedException {

        var client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(SseClient.class);
        given()
                .accept(MediaType.SERVER_SENT_EVENTS)
                .when()
                .get("/schedules/" + UUID.randomUUID() + "/events")
                .then()
                .statusCode(404);
        EmployeeSchedule inputSchedule = createInputEmployeeSchedule();
        Metadata<HardMediumSoftScore> metadata = post(inputSchedule, OperationOnPost.NONE);

        AtomicReference<Throwable> eventStreamError = new AtomicReference<>();
        List<Metadata<HardMediumSoftScore>> receivedResults = new ArrayList<>();

        CountDownLatch waitOnRequestSubscribtion = new CountDownLatch(1);
        Multi<SseEvent<Metadata<HardMediumSoftScore>>> eventStream = client.getEvents(modelApiVersion, metadata.getId());

        Cancellable subscription = eventStream.subscribe().with(
                event -> {
                    System.out.println("event" + event.data().getSolverStatus());
                    receivedResults.add(event.data());
                },
                eventStreamError::set);
        eventStream.onSubscription().invoke(waitOnRequestSubscribtion::countDown);
        waitOnRequestSubscribtion.await(2, TimeUnit.SECONDS);

        Metadata<HardMediumSoftScore> solveMetadata = post(metadata);
        awaitFeasiblyAssigned(solveMetadata);

        ModelResponse<HardMediumSoftScore, EmployeeSchedule, EmployeeScheduleInputMetrics, EmployeeScheduleOutputMetrics> response =
                getModelResponse(metadata);

        assertAllShiftsAssigned(response.modelOutput());

        Set<SolvingStatus> distinctStatuses =
                receivedResults.stream().map(Metadata::getSolverStatus)
                        .collect(Collectors.toSet());
        assertThat(distinctStatuses).contains(SolvingStatus.DATASET_COMPUTED, SolvingStatus.SOLVING_STARTED,
                SolvingStatus.SOLVING_ACTIVE, SolvingStatus.SOLVING_COMPLETED);
        assertThat(eventStreamError).hasNullValue();
        subscription.cancel();

        given()
                .accept(MediaType.SERVER_SENT_EVENTS)
                .when()
                .get("/schedules/" + metadata.getId() + "/events")
                .then()
                .statusCode(410);
    }

    @Test
    void getValidationResult() {
        EmployeeSchedule inputSchedule = createInputEmployeeSchedule();
        Shift wrongShift = new Shift("Wrong shift", Skill.AMBULANCE).withStartAndEndTime(TOMORROW_16_00, TOMORROW_08_00);
        inputSchedule.getShifts().add(wrongShift);

        var metadata = post(inputSchedule, OperationOnPost.NONE);

        var validationResult = getValidationResult(metadata);
        assertThat(validationResult).isNotNull();

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.issues()).hasSize(1);

        var abstractIssue = validationResult.issues().iterator().next();

        assertThat(abstractIssue).isInstanceOf(ShiftEndBeforeStartIssue.class);
        var issue = (ShiftEndBeforeStartIssue) abstractIssue;
        assertThat(issue.getCode().value()).isEqualTo("SHIFT_END_BEFORE_START");
        assertThat(issue.getShiftId()).isEqualTo(wrongShift.getId());
        assertThat(issue.getStartTime()).isEqualTo(wrongShift.getStartTime());
        assertThat(issue.getEndTime()).isEqualTo(wrongShift.getEndTime());
    }

    @Test
    void getIssueTypes() {
        var validationIssueTypes = given()
                .accept(ContentType.JSON)
                .when()
                .get("/schedules/validation-issue-types")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract()
                .as(ValidationIssueTypes.class);

        assertThat(validationIssueTypes).isNotNull();
        assertThat(validationIssueTypes.issueTypes()).hasSize(1);
        IssueType issueType = validationIssueTypes.issueTypes().iterator().next();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(issueType.code()).isEqualTo(ShiftEndBeforeStartIssue.ISSUE_CODE);
            softly.assertThat(issueType.severity()).isEqualTo(IssueSeverity.ERROR);
            softly.assertThat(issueType.metadata()).hasSize(1).containsExactly(ShiftEndBeforeStartIssue.ISSUE_MESSAGE);
        });
    }

    @Test
    void getIssueTypeByCode() {
        IssueCode issueCode = ShiftEndBeforeStartIssue.ISSUE_CODE;
        var issueType = given()
                .accept(ContentType.JSON)
                .when()
                .get("/schedules/validation-issue-types/" + issueCode.value())
                .then()
                .log().ifError()
                .statusCode(200)
                .extract()
                .as(IssueType.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(issueType.code()).isEqualTo(ShiftEndBeforeStartIssue.ISSUE_CODE);
            softly.assertThat(issueType.severity()).isEqualTo(IssueSeverity.ERROR);
            softly.assertThat(issueType.metadata()).hasSize(1).containsExactly(ShiftEndBeforeStartIssue.ISSUE_MESSAGE);
        });
    }

    private static EmployeeSchedule awaitFeasiblyAssigned(Metadata<HardMediumSoftScore> metadata) {
        await()
                .atMost(TEST_AWAIT_TIMEOUT_DURATION)
                .pollInterval(TEST_POLL_INTERVAL_MILLIS)
                .until(() -> {
                    EmployeeSchedule resultEmployeeSchedule = getOutputEmployeeSchedule(metadata);
                    return resultEmployeeSchedule.getScore().isFeasible()
                            && resultEmployeeSchedule.getScore().mediumScore() == 0L;
                });

        return getOutputEmployeeSchedule(metadata);
    }

    private static
            ModelResponse<HardMediumSoftScore, EmployeeSchedule, EmployeeScheduleInputMetrics, EmployeeScheduleOutputMetrics>
            getModelResponse(Metadata<HardMediumSoftScore> metadata) {
        return given()
                .accept(ContentType.JSON)
                .when()
                .get("/schedules/" + metadata.getId())
                .then()
                .log().ifError()
                .statusCode(200)
                .extract()
                .as(new TypeRef<>() {
                });
    }

    private static EmployeeSchedule getOutputEmployeeSchedule(Metadata<HardMediumSoftScore> metadata) {
        ModelResponse<HardMediumSoftScore, EmployeeSchedule, EmployeeScheduleInputMetrics, EmployeeScheduleOutputMetrics> modelResponse =
                getModelResponse(metadata);

        return modelResponse.modelOutput();
    }

    private static Metadata<HardMediumSoftScore> post(EmployeeSchedule inputSchedule) {
        return post(inputSchedule, OperationOnPost.SOLVE);
    }

    private static Metadata<HardMediumSoftScore> post(EmployeeSchedule inputSchedule,
            OperationOnPost operationOnPost) {
        ModelRequest<EmployeeSchedule, EmptyModelConfigOverrides> modelRequest = new ModelRequest<>(inputSchedule);

        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(modelRequest)
                .when()
                .post("/schedules?operation=" + operationOnPost.name())
                .then()
                .log().ifError()
                .statusCode(202)
                .extract()
                .as(new TypeRef<>() {
                });
    }

    private static Metadata<HardMediumSoftScore> post(Metadata<HardMediumSoftScore> metadata) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(metadata)
                .when()
                .post("/schedules/" + metadata.getId())
                .then()
                .log().ifError()
                .statusCode(202)
                .extract()
                .as(new TypeRef<>() {
                });
    }

    private static ValidationResult<?> getValidationResult(Metadata<HardMediumSoftScore> metadata) {
        return given()
                .accept(ContentType.JSON)
                .when()
                .get("/schedules/" + metadata.getId() + "/validation-result")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract()
                .as(new TypeRef<ValidationResult<ShiftEndBeforeStartIssue>>() {
                });
    }

    private EmployeeSchedule createInputEmployeeSchedule() {
        Employee employeeAnn = new Employee("Ann", Skill.EMERGENCY, Skill.AMBULANCE);
        Employee employeeBeth = new Employee("Beth", Skill.AMBULANCE, Skill.DERMATOLOGY);
        Employee employeeCarl = new Employee("Carl", Skill.CARDIOLOGY, Skill.ANESTHESIA);

        List<Employee> employees = List.of(employeeAnn, employeeBeth, employeeCarl);

        Shift ambulanceShift1 =
                new Shift("Ambulance shift 1", Skill.AMBULANCE).withStartAndEndTime(TOMORROW_08_00, TOMORROW_16_00);
        Shift dermatologyShift1 =
                new Shift("Dermatology shift 1", Skill.DERMATOLOGY).withStartAndEndTime(TOMORROW_08_00, TOMORROW_16_00);
        Shift cardiologyShift1 =
                new Shift("Cardiology shift 1", Skill.CARDIOLOGY).withStartAndEndTime(TOMORROW_08_00, TOMORROW_16_00);
        Shift anesthesiaShift1 =
                new Shift("Anesthesia shift 1", Skill.ANESTHESIA).withStartAndEndTime(TOMORROW_08_00, TOMORROW_16_00);

        List<Shift> shifts = List.of(
                ambulanceShift1,
                dermatologyShift1,
                cardiologyShift1,
                anesthesiaShift1);

        return new EmployeeSchedule(employees, new ArrayList<>(shifts));
    }

    private void assertAllShiftsAssigned(EmployeeSchedule employeeSchedule) {
        SoftAssertions.assertSoftly(softly -> {
            employeeSchedule.getShifts().forEach(shift -> {
                softly.assertThat(shift.getEmployee())
                        .as("Shift (%s) should have an assigned employee", shift.getId())
                        .isNotNull();
            });
        });
    }
}
