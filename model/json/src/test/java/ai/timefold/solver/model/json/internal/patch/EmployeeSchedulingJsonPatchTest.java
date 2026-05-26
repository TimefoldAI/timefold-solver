package ai.timefold.solver.model.json.internal.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EmployeeSchedulingJsonPatchTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testAddEmployee() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/employees/-", "value": {"id": "john doe","contracts": [
                                "fullTimeContract"
                            ],
                            "skills": [
                                {
                                    "id": "Ambulance"
                                }
                            ]} }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees + 1, numberOfEmployeesAfterPatch);

        JsonNode addedEmployee = modelInputPatched.get("employees").get(numberOfEmployeesAfterPatch - 1);
        assertEquals("john doe", addedEmployee.get("id").asText());
        assertEquals("fullTimeContract", addedEmployee.get("contracts").get(0).asText());
        assertEquals("Ambulance", addedEmployee.get("skills").get(0).get("id").asText());
    }

    @Test
    public void testAddContractToEmployee() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/employees/[id=Ann Cole]/contracts/-", "value": "test" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        JsonNode employee = modelInputPatched.get("employees").get(0);
        assertEquals("fullTimeContract", employee.get("contracts").get(0).asText());
        assertEquals("test", employee.get("contracts").get(1).asText());
    }

    @Test
    public void testAddUnavailableTimeSpansToEmployee() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/employees/[id=Ann Cole]/unavailableTimeSpans/-", "value": {
                         "start": "2027-02-01T00:00:00Z",
                         "end": "2027-02-02T00:00:00Z"
                       } }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        JsonNode employee = modelInputPatched.get("employees").get(0);
        assertEquals(1, employee.get("unavailableTimeSpans").size());
    }

    @Test
    public void testRemoveContractFromEmployee() throws IOException {

        String patch = """
                [
                  { "op": "remove", "path": "/employees/[id=Ann Cole]/contracts/0" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        JsonNode employee = modelInputPatched.get("employees").get(0);
        assertEquals(0, employee.get("contracts").size());
    }

    @Test
    public void testRemoveContractFromEmployeeByName() throws IOException {

        String patch = """
                [
                  { "op": "remove", "path": "/employees/[id=Ann Cole]/contracts/[fullTimeContract]" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        JsonNode employee = modelInputPatched.get("employees").get(0);
        assertEquals(0, employee.get("contracts").size());
    }

    @Test
    public void testAddSkillEmployee() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/employees/[id=Ann Cole]/skills/-", "value": {"id": "Test"}}
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        ObjectNode emp = (ObjectNode) modelInputPatched.at("/employees/0");
        ArrayNode skills = (ArrayNode) emp.get("skills");
        assertEquals(2, skills.size());

        List<String> skillIds = skills.valueStream().map(item -> item.get("id").asText()).toList();
        assertIterableEquals(List.of("Ambulance", "Test"), skillIds);
    }

    @Test
    public void testAddSkillEmployeeNotExisting() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/employees/[id=Not existing]/skills/-", "value": {"id": "Test"}}
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

    }

    @Test
    public void testAddPreferedTimeSpanForEmployee() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/employees/[id=Ann Cole]/preferredTimeSpans/-", "value": {
                  "start" : "2025-08-05T00:00:00-04:00",
                  "end" : "2025-08-07T00:00:00-04:00",
                  "includeShiftTags" : [ "Night" ]
                }}
                              ]
                              """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        ObjectNode emp = (ObjectNode) modelInputPatched.at("/employees/0");
        ArrayNode preferredTimeSpans = (ArrayNode) emp.get("preferredTimeSpans");
        assertEquals(3, preferredTimeSpans.size());
    }

    @Test
    public void testRemoveSkillFromEmployee() throws IOException {

        String patch = """
                [
                  { "op": "remove", "path": "/employees/[id=Ann Cole]/skills/0"}
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        ObjectNode emp = (ObjectNode) modelInputPatched.at("/employees/0");
        ArrayNode skills = (ArrayNode) emp.get("skills");
        assertEquals(0, skills.size());
    }

    @Test
    public void testRemoveSkillFromEmployeeBySkillId() throws IOException {

        String patch = """
                [
                  { "op": "remove", "path": "/employees/[id=Ann Cole]/skills/[id=Ambulance]"}
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        ObjectNode emp = (ObjectNode) modelInputPatched.at("/employees/0");
        ArrayNode skills = (ArrayNode) emp.get("skills");
        assertEquals(0, skills.size());
    }

    @Test
    public void testReplaceSkillEmployee() throws IOException {

        String patch = """
                [
                  { "op": "replace", "path": "/employees/[id=Ann Cole]/skills", "value": [{"id": "Test"}]}
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        ObjectNode emp = (ObjectNode) modelInputPatched.at("/employees/0");
        ArrayNode skills = (ArrayNode) emp.get("skills");
        assertEquals(1, skills.size());

        assertEquals("Test", skills.get(0).get("id").textValue());
    }

    @Test
    public void testReplaceTimeZoneForEmployee() throws IOException {

        String patch = """
                [
                  { "op": "replace", "path": "/employees/[id=Ann Cole]/timeZoneId", "value": "-08:00"}
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees, numberOfEmployeesAfterPatch);

        ObjectNode emp = (ObjectNode) modelInputPatched.at("/employees/0");
        String timezoneId = emp.get("timeZoneId").textValue();
        assertEquals("-08:00", timezoneId);
    }

    @Test
    public void testRemoveEmployeeWithSkill() throws IOException {
        String patch = """
                [
                { "op": "remove", "path": "/employees/[skills/id=Ambulance]" }
                ]
                """;

        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees - 5, numberOfEmployeesAfterPatch);
    }

    @Test
    public void testRemoveEmployeeWithId() throws IOException {
        String patch = """
                [
                { "op": "remove", "path": "/employees/[id=Ann Cole]" }
                ]
                """;

        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees - 1, numberOfEmployeesAfterPatch);
    }

    @Test
    public void testRemoveEmployeeByIndex() throws IOException {
        String patch = """
                [
                { "op": "remove", "path": "/employees/0" }
                ]
                """;

        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfEmployees = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfEmployees - 1, numberOfEmployeesAfterPatch);
    }

    @Test
    public void testAddShift() throws IOException {

        String patch = """
                        [
                          { "op": "add", "path": "/shifts/-", "value": {
                    "id": "Sun M Ambulance",
                    "start": "2025-08-10T06:00:00-04:00",
                    "end": "2025-08-10T14:00:00-04:00",
                    "requiredSkills": [
                        "Ambulance"
                    ],
                    "tags": [
                        "Morning"
                    ]
                } }
                        ]
                        """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfShifts = modelInput.get("shifts").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfShiftsAfterPatch = modelInputPatched.get("shifts").size();

        assertEquals(numberOfShifts + 1, numberOfShiftsAfterPatch);

        JsonNode addedShift = modelInputPatched.get("shifts").get(numberOfShiftsAfterPatch - 1);
        assertEquals("Sun M Ambulance", addedShift.get("id").asText());
        assertEquals("Ambulance", addedShift.get("requiredSkills").get(0).asText());
        assertEquals("Morning", addedShift.get("tags").get(0).asText());
    }

    @Test
    public void testAddTagToShift() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/shifts/[id=Mon M Ambulance]/tags/-", "value": "Night" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfShifts = modelInput.get("shifts").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("shifts").size();

        assertEquals(numberOfShifts, numberOfEmployeesAfterPatch);

        JsonNode shifts = modelInputPatched.get("shifts").get(0);
        assertEquals(2, shifts.get("tags").size());
        assertEquals("Morning", shifts.get("tags").get(0).asText());
        assertEquals("Night", shifts.get("tags").get(1).asText());
    }

    @Test
    public void testAddTagToEmployee() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/employees/[id=Ann Cole]/tags/-", "value": "Night" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfShifts = modelInput.get("employees").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("employees").size();

        assertEquals(numberOfShifts, numberOfEmployeesAfterPatch);

        JsonNode tags = modelInputPatched.get("employees").get(0);
        assertEquals(1, tags.get("tags").size());
        assertEquals("Night", tags.get("tags").get(0).asText());

        patch = """
                [
                  { "op": "replace", "path": "/employees/[id=Ann Cole]/tags", "value": ["Day"] }
                ]
                """;

        modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        tags = modelInputPatched.get("employees").get(0);
        assertEquals(1, tags.get("tags").size());
        assertEquals("Day", tags.get("tags").get(0).asText());
    }

    @Test
    public void testAddRequiredSkillsToShift() throws IOException {

        String patch = """
                [
                  { "op": "add", "path": "/shifts/[id=Mon M Ambulance]/requiredSkills/-", "value": "Surgery" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfShifts = modelInput.get("shifts").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("shifts").size();

        assertEquals(numberOfShifts, numberOfEmployeesAfterPatch);

        JsonNode shifts = modelInputPatched.get("shifts").get(0);
        assertEquals(2, shifts.get("requiredSkills").size());
        assertEquals("Ambulance", shifts.get("requiredSkills").get(0).asText());
        assertEquals("Surgery", shifts.get("requiredSkills").get(1).asText());
    }

    @Test
    public void testRemoveRequiredSkillsFromShift() throws IOException {

        String patch = """
                [
                  { "op": "remove", "path": "/shifts/[id=Mon M Ambulance]/requiredSkills/0" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfShifts = modelInput.get("shifts").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("shifts").size();

        assertEquals(numberOfShifts, numberOfEmployeesAfterPatch);

        JsonNode shifts = modelInputPatched.get("shifts").get(0);
        assertEquals(0, shifts.get("requiredSkills").size());
    }

    @Test
    public void testRemoveRequiredSkillsFromShiftByName() throws IOException {

        String patch = """
                [
                  { "op": "remove", "path": "/shifts/[id=Mon M Ambulance]/requiredSkills/Ambulance" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfShifts = modelInput.get("shifts").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("shifts").size();

        assertEquals(numberOfShifts, numberOfEmployeesAfterPatch);

        JsonNode shifts = modelInputPatched.get("shifts").get(0);
        assertEquals(0, shifts.get("requiredSkills").size());
    }

    @Test
    public void testRemoveShiftById() throws IOException {

        String patch = """
                [
                  { "op": "remove", "path": "/shifts/[id=Mon M Ambulance]" }
                ]
                """;
        JsonNode modelInput = mapper.readTree(
                this.getClass().getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/CONTRACT_RULES_input.json"));

        int numberOfShifts = modelInput.get("shifts").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfEmployeesAfterPatch = modelInputPatched.get("shifts").size();

        assertEquals(numberOfShifts - 1, numberOfEmployeesAfterPatch);
    }
}
