package ai.timefold.solver.model.json.internal.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class FieldServiceRoutingJsonPatchTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testAddVehicle() throws IOException {

        String patch = """
                [
                          { "op": "add", "path": "/vehicles/-", "value": {
                    "id": "AnnExtra",
                    "vehicleType": "VAN",
                    "shifts": [
                        {
                            "id": "cd4c29ed",
                            "startLocation": [
                                33.771758735816796,
                                -84.37174927400983
                            ],
                            "minStartTime": "2025-08-18T08:00:00-04:00",
                            "maxEndTime": "2025-08-18T17:00:00-04:00",
                            "skills": [
                                {
                                    "name": "electrician",
                                    "level": 1,
                                    "multiplier": null
                                }
                            ],
                            "tags": [
                            ],
                            "requiredBreaks": [
                                {
                                    "id": "cd4c29ed Lunch",
                                    "minStartTime": "2025-08-18T12:00:00-04:00",
                                    "maxEndTime": null,
                                    "duration": "PT1H",
                                    "costImpact": "PAID",
                                    "type": "FLOATING"
                                }
                            ],
                            "temporarySkillSets": [
                            ],
                            "temporaryTagSets": [
                            ],
                            "itinerary": [
                            ]
                        }
                    ],
                    "historicalTimeUtilized": "PT0S",
                    "historicalTimeCapacity": "PT0S"
                } }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles + 1, numberOfVehiclesAfterPatch);

        JsonNode addedVehicle = modelInputPatched.get("vehicles").get(numberOfVehiclesAfterPatch - 1);
        assertEquals("AnnExtra", addedVehicle.get("id").asText());
        assertEquals("VAN", addedVehicle.get("vehicleType").asText());
    }

    @Test
    public void testAddShiftToVehicle() throws IOException {

        String patch = """
                [
                     { "op": "add", "path": "/vehicles/[id=Ann]/shifts/-", "value": {
                            "id": "cd4c29ex",
                            "startLocation": [
                                33.771758735816796,
                                -84.37174927400983
                            ],
                            "minStartTime": "2025-08-18T08:00:00-04:00",
                            "maxEndTime": "2025-08-18T17:00:00-04:00",
                            "skills": [
                                {
                                    "name": "electrician",
                                    "level": 1,
                                    "multiplier": null
                                }
                            ],
                            "tags": [
                            ],
                            "requiredBreaks": [
                                {
                                    "id": "cd4c29ed Lunch",
                                    "minStartTime": "2025-08-18T12:00:00-04:00",
                                    "maxEndTime": null,
                                    "duration": "PT1H",
                                    "costImpact": "PAID",
                                    "type": "FLOATING"
                                }
                            ],
                            "temporarySkillSets": [
                            ],
                            "temporaryTagSets": [
                            ],
                            "itinerary": [
                            ]
                        }}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(2, vehicle.get("shifts").size());

    }

    @Test
    public void testAddTagToVehicleByShiftIndex() throws IOException {

        String patch = """
                [
                     { "op": "add", "path": "/vehicles/[id=Ann]/shifts/0/tags/-", "value": "emergency"}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(1, vehicle.at("/shifts/0/tags").size());
        assertEquals("emergency", vehicle.at("/shifts/0/tags").get(0).asText());

    }

    @Test
    public void testAddTagToVehicleByShiftId() throws IOException {

        String patch = """
                [
                     { "op": "add", "path": "/vehicles/[id=Ann]/shifts/[id=cd4c29ed]/tags/-", "value": "emergency"}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(1, vehicle.at("/shifts/0/tags").size());
        assertEquals("emergency", vehicle.at("/shifts/0/tags").get(0).asText());

    }

    @Test
    public void testAddSkillToVehicleByShiftId() throws IOException {

        String patch = """
                [
                     { "op": "add", "path": "/vehicles/[id=Ann]/shifts/[id=cd4c29ed]/skills/-", "value": {
                                    "name": "musician",
                                    "level": 1,
                                    "multiplier": null
                                }}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(2, vehicle.at("/shifts/0/skills").size());
        assertEquals("electrician", vehicle.at("/shifts/0/skills").get(0).get("name").asText());
        assertEquals("musician", vehicle.at("/shifts/0/skills").get(1).get("name").asText());

    }

    @Test
    public void testReplaceSkillsForVehicleByShiftId() throws IOException {

        String patch = """
                [
                     { "op": "replace", "path": "/vehicles/[id=Ann]/shifts/[id=cd4c29ed]/skills", "value": [{
                                    "name": "musician",
                                    "level": 1,
                                    "multiplier": null
                                }]}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(1, vehicle.at("/shifts/0/skills").size());
        assertEquals("musician", vehicle.at("/shifts/0/skills").get(0).get("name").asText());

    }

    @Test
    public void testReplaceRequiredBreaksForVehicleByShiftId() throws IOException {

        String patch = """
                [
                     { "op": "replace", "path": "/vehicles/[id=Ann]/shifts/[id=cd4c29ed]/requiredBreaks", "value": [
                                {
                                    "id": "cd4c29ed Lunch",
                                    "minStartTime": "2025-08-18T12:00:00-04:00",
                                    "maxEndTime": null,
                                    "duration": "PT1H",
                                    "costImpact": "PAID",
                                    "type": "FLOATING"
                                },
                                {
                                    "id": "cd4c29ed Dinner",
                                    "minStartTime": "2025-08-18T18:00:00-04:00",
                                    "maxEndTime": null,
                                    "duration": "PT1H",
                                    "costImpact": "PAID",
                                    "type": "FLOATING"
                                }
                            ]}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(2, vehicle.at("/shifts/0/requiredBreaks").size());
        assertEquals("cd4c29ed Lunch", vehicle.at("/shifts/0/requiredBreaks").get(0).get("id").asText());
        assertEquals("cd4c29ed Dinner", vehicle.at("/shifts/0/requiredBreaks").get(1).get("id").asText());
    }

    @Test
    public void testRemoveRequiredBreaksForVehicleByShiftId() throws IOException {

        String patch = """
                [
                     { "op": "remove", "path": "/vehicles/[id=Ann]/shifts/[id=cd4c29ed]/requiredBreaks/[id=cd4c29ed Lunch]"}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(0, vehicle.at("/shifts/0/requiredBreaks").size());
    }

    @Test
    public void testRemoveRequiredBreaksForVehicleByShiftIdIndex() throws IOException {

        String patch = """
                [
                     { "op": "remove", "path": "/vehicles/[id=Ann]/shifts/[id=cd4c29ed]/requiredBreaks/0"}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(0, vehicle.at("/shifts/0/requiredBreaks").size());
    }

    @Test
    public void testRemoveShiftForVehicleByShiftId() throws IOException {

        String patch = """
                [
                     { "op": "remove", "path": "/vehicles/[id=Ann]/shifts/[id=cd4c29ed]"}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(0, vehicle.at("/shifts").size());
    }

    @Test
    public void testRemoveShiftForVehicleByIndex() throws IOException {

        String patch = """
                [
                     { "op": "remove", "path": "/vehicles/[id=Ann]/shifts/0"}
                ]

                """;

        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVehicles = modelInput.get("vehicles").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);
        int numberOfVehiclesAfterPatch = modelInputPatched.get("vehicles").size();

        assertEquals(numberOfVehicles, numberOfVehiclesAfterPatch);

        JsonNode vehicle = modelInputPatched.get("vehicles").get(0);
        assertEquals(0, vehicle.at("/shifts").size());
    }

    @Test
    public void testAddVisit() throws IOException {

        String patch = """
                [
                          { "op": "add", "path": "/visits/-", "value": {
                        "id": "6e57fcdx",
                        "name": "Cole Inc. extra",
                        "location": [
                            33.68906251585709,
                            -84.44268080179887
                        ],
                        "timeWindows": [
                            {
                                "minStartTime": "2025-08-18T09:00:00-04:00",
                                "maxEndTime": "2025-08-18T17:00:00-04:00"
                            }
                        ],
                        "serviceDuration": "PT1H15M",
                        "requiredSkills": [
                            {
                                "name": "plumber",
                                "minLevel": null
                            }
                        ],
                        "priority": "10",
                        "pinningRequested": false
                    } }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits + 1, numberOfVisitsAfterPatch);

        JsonNode addedVisit = modelInputPatched.get("visits").get(numberOfVisitsAfterPatch - 1);
        assertEquals("6e57fcdx", addedVisit.get("id").asText());
        assertEquals("Cole Inc. extra", addedVisit.get("name").asText());
    }

    @Test
    public void testAddVisitsRequiredSkill() throws IOException {

        String patch = """
                [
                          { "op": "add", "path": "/visits/[id=6e57fcd4]/requiredSkills/-", "value": {
                                "name": "electrician",
                                "minLevel": null
                            }
                          }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits, numberOfVisitsAfterPatch);

        JsonNode visit = modelInputPatched.get("visits").get(0);
        assertEquals(2, visit.at("/requiredSkills").size());
        assertEquals("plumber", visit.at("/requiredSkills").get(0).get("name").asText());
        assertEquals("electrician", visit.at("/requiredSkills").get(1).get("name").asText());
    }

    @Test
    public void testRemoveVisitsRequiredSkill() throws IOException {

        String patch = """
                [
                          { "op": "remove", "path": "/visits/[id=6e57fcd4]/requiredSkills/[name=plumber]"}
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits, numberOfVisitsAfterPatch);

        JsonNode visit = modelInputPatched.get("visits").get(0);
        assertEquals(0, visit.at("/requiredSkills").size());
    }

    @Test
    public void testRemoveVisitsById() throws IOException {

        String patch = """
                [
                          { "op": "remove", "path": "/visits/[id=6e57fcd4]"}
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits - 1, numberOfVisitsAfterPatch);
    }

    @Test
    public void testReplaceVisitsById() throws IOException {

        String patch = """
                [
                          { "op": "replace", "path": "/visits/[id=6e57fcd4]", "value": {
                        "id": "6e57fcdx",
                        "name": "Cole Inc. extra",
                        "location": [
                            33.68906251585709,
                            -84.44268080179887
                        ],
                        "timeWindows": [
                            {
                                "minStartTime": "2025-08-18T09:00:00-04:00",
                                "maxEndTime": "2025-08-18T17:00:00-04:00"
                            }
                        ],
                        "serviceDuration": "PT1H15M",
                        "requiredSkills": [
                            {
                                "name": "plumber",
                                "minLevel": null
                            }
                        ],
                        "priority": "10",
                        "pinningRequested": false
                    }}

                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits, numberOfVisitsAfterPatch);

        JsonNode addedVisit = modelInputPatched.get("visits").get(numberOfVisitsAfterPatch - 1);
        assertEquals("6e57fcdx", addedVisit.get("id").asText());
        assertEquals("Cole Inc. extra", addedVisit.get("name").asText());
    }

    @Test
    public void testReplaceVisitsByIndex() throws IOException {

        String patch = """
                [
                          { "op": "replace", "path": "/visits/0", "value": {
                        "id": "6e57fcdx",
                        "name": "Cole Inc. extra",
                        "location": [
                            33.68906251585709,
                            -84.44268080179887
                        ],
                        "timeWindows": [
                            {
                                "minStartTime": "2025-08-18T09:00:00-04:00",
                                "maxEndTime": "2025-08-18T17:00:00-04:00"
                            }
                        ],
                        "serviceDuration": "PT1H15M",
                        "requiredSkills": [
                            {
                                "name": "plumber",
                                "minLevel": null
                            }
                        ],
                        "priority": "10",
                        "pinningRequested": false
                    }}

                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits, numberOfVisitsAfterPatch);

        JsonNode addedVisit = modelInputPatched.get("visits").get(0);
        assertEquals("6e57fcdx", addedVisit.get("id").asText());
        assertEquals("Cole Inc. extra", addedVisit.get("name").asText());
    }

    @Test
    public void testReplaceVisitsPriorityById() throws IOException {

        String patch = """
                [
                          { "op": "replace", "path": "/visits/[id=6e57fcd4]/priority", "value": "5"}

                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits, numberOfVisitsAfterPatch);

        JsonNode visit = modelInputPatched.get("visits").get(0);
        assertEquals("5", visit.get("priority").asText());
    }

    @Test
    public void testReplaceVisitsLocationById() throws IOException {

        String patch = """
                [
                          { "op": "replace", "path": "/visits/[id=6e57fcd4]/location", "value": [
                            50.68906251585709,
                            -90.44268080179887
                        ]}

                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits, numberOfVisitsAfterPatch);

        JsonNode visit = modelInputPatched.get("visits").get(0);
        assertEquals("50.68906251585709", visit.get("location").get(0).asText());
        assertEquals("-90.44268080179887", visit.get("location").get(1).asText());
    }

    @Test
    public void testReplaceVisitsPinnedResourceByPriority() throws IOException {

        String patch = """
                [
                          { "op": "replace", "path": "/visits/[priority=6]/pinningRequested", "value": true}

                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisits = modelInput.get("visits").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitsAfterPatch = modelInputPatched.get("visits").size();

        assertEquals(numberOfVisits, numberOfVisitsAfterPatch);

        List<Boolean> pinnedRequestedForPriority6 =
                modelInputPatched.get("visits").valueStream().filter(item -> item.get("priority").asText().equals("6"))
                        .map(item -> item.get("pinningRequested").asBoolean()).toList();
        assertEquals(10, pinnedRequestedForPriority6.size());
        assertTrue(pinnedRequestedForPriority6.stream().allMatch(v -> v == true));
    }

    @Test
    public void testAddVisitGroup() throws IOException {

        String patch = """
                        [
                                  { "op": "add", "path": "/visitGroups/-", "value": {
                    "id": "ce9aa5ex",
                    "alignment": "START",
                    "serviceDurationStrategy": "INDIVIDUAL",
                    "visits": [
                        {
                            "id": "0d7f6e52",
                            "name": "Jay Robinson 1 / 3",
                            "location": [
                                33.80118537771744,
                                -84.40335014628359
                            ],
                            "timeWindows": [
                                {
                                    "minStartTime": "2025-08-18T08:00:00-04:00",
                                    "maxEndTime": "2025-08-18T16:00:00-04:00"
                                }
                            ],
                            "serviceDuration": "PT1H30M",
                            "requiredSkills": [
                                {
                                    "name": "plumber",
                                    "minLevel": null
                                }
                            ],
                            "priority": "8",
                            "pinningRequested": false
                        }
                    ]
                } }
                        ]
                        """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisitGroups = modelInput.get("visitGroups").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitGroupsAfterPatch = modelInputPatched.get("visitGroups").size();

        assertEquals(numberOfVisitGroups + 1, numberOfVisitGroupsAfterPatch);

        JsonNode addedVisitGroup = modelInputPatched.get("visitGroups").get(numberOfVisitGroupsAfterPatch - 1);
        assertEquals("ce9aa5ex", addedVisitGroup.get("id").asText());
        assertEquals(1, addedVisitGroup.get("visits").size());
    }

    @Test
    public void testAddVisitToVisitGroup() throws IOException {

        String patch = """
                [
                          { "op": "add", "path": "/visitGroups/[id=ce9aa5e4]/visits/-", "value": {
                    "id": "0d7f6e5x",
                    "name": "Mick Robinson 1 / 3",
                    "location": [
                        33.80118537771744,
                        -84.40335014628359
                    ],
                    "timeWindows": [
                        {
                            "minStartTime": "2025-08-18T08:00:00-04:00",
                            "maxEndTime": "2025-08-18T16:00:00-04:00"
                        }
                    ],
                    "serviceDuration": "PT1H30M",
                    "requiredSkills": [
                        {
                            "name": "plumber",
                            "minLevel": null
                        }
                    ],
                    "priority": "8",
                    "pinningRequested": false
                }
                }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisitGroups = modelInput.get("visitGroups").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitGroupsAfterPatch = modelInputPatched.get("visitGroups").size();

        assertEquals(numberOfVisitGroups, numberOfVisitGroupsAfterPatch);

        JsonNode visitGroup = modelInputPatched.get("visitGroups").get(0);
        assertEquals("ce9aa5e4", visitGroup.get("id").asText());
        assertEquals(4, visitGroup.get("visits").size());
        assertEquals("0d7f6e5x", visitGroup.get("visits").get(3).get("id").asText());
        assertEquals("Mick Robinson 1 / 3", visitGroup.get("visits").get(3).get("name").asText());
    }

    @Test
    public void testReplaceVisitFromVisitGroup() throws IOException {

        String patch = """
                [
                          { "op": "replace", "path": "/visitGroups/[id=ce9aa5e4]/visits/[id=0d7f6e52]", "value": {
                    "id": "0d7f6e5x",
                    "name": "Mick Robinson 1 / 3",
                    "location": [
                        33.80118537771744,
                        -84.40335014628359
                    ],
                    "timeWindows": [
                        {
                            "minStartTime": "2025-08-18T08:00:00-04:00",
                            "maxEndTime": "2025-08-18T16:00:00-04:00"
                        }
                    ],
                    "serviceDuration": "PT1H30M",
                    "requiredSkills": [
                        {
                            "name": "plumber",
                            "minLevel": null
                        }
                    ],
                    "priority": "8",
                    "pinningRequested": false
                }
                }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisitGroups = modelInput.get("visitGroups").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitGroupsAfterPatch = modelInputPatched.get("visitGroups").size();

        assertEquals(numberOfVisitGroups, numberOfVisitGroupsAfterPatch);

        JsonNode visitGroup = modelInputPatched.get("visitGroups").get(0);
        assertEquals("ce9aa5e4", visitGroup.get("id").asText());
        assertEquals(3, visitGroup.get("visits").size());
        assertEquals("0d7f6e5x", visitGroup.get("visits").get(2).get("id").asText());
        assertEquals("Mick Robinson 1 / 3", visitGroup.get("visits").get(2).get("name").asText());
    }

    @Test
    public void testRemoveVisitFromVisitGroup() throws IOException {

        String patch = """
                [
                          { "op": "remove", "path": "/visitGroups/[id=ce9aa5e4]/visits/[id=0d7f6e52]" }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisitGroups = modelInput.get("visitGroups").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitGroupsAfterPatch = modelInputPatched.get("visitGroups").size();

        assertEquals(numberOfVisitGroups, numberOfVisitGroupsAfterPatch);

        JsonNode visitGroup = modelInputPatched.get("visitGroups").get(0);
        assertEquals("ce9aa5e4", visitGroup.get("id").asText());
        assertEquals(2, visitGroup.get("visits").size());
    }

    @Test
    public void testRemoveSkill() throws IOException {

        String patch = """
                [
                          { "op": "remove", "path": "/skills/[electrician]" }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisitGroups = modelInput.get("visitGroups").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitGroupsAfterPatch = modelInputPatched.get("visitGroups").size();

        assertEquals(numberOfVisitGroups, numberOfVisitGroupsAfterPatch);

        JsonNode skills = modelInputPatched.get("skills");
        assertEquals(3, skills.size());
    }

    @Test
    public void testAddSkill() throws IOException {

        String patch = """
                [
                          { "op": "add", "path": "/skills/-", "value": "musician" }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        int numberOfVisitGroups = modelInput.get("visitGroups").size();

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        int numberOfVisitGroupsAfterPatch = modelInputPatched.get("visitGroups").size();

        assertEquals(numberOfVisitGroups, numberOfVisitGroupsAfterPatch);

        JsonNode skills = modelInputPatched.get("skills");
        assertEquals(5, skills.size());
    }

    @Test
    public void testAddFreezeDeparturesBeforeTime() throws IOException {

        String patch = """
                [
                      { "op": "add", "path": "/freezeDeparturesBeforeTime", "value": "2027-02-01T14:10:00Z" }
                ]
                """;
        JsonNode modelInput = mapper
                .readTree(this.getClass()
                        .getResourceAsStream("/ai/timefold/solver/model/json/internal/patch/VISIT_GROUP_input.json"));

        JsonNode modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInput);

        JsonNode freezeDeparturesBeforeTime = modelInputPatched.get("freezeDeparturesBeforeTime");
        assertEquals("2027-02-01T14:10:00Z", freezeDeparturesBeforeTime.textValue());

        patch = """
                [
                      { "op": "remove", "path": "/freezeDeparturesBeforeTime" }
                ]
                """;
        modelInputPatched = JsonPatch.apply((ArrayNode) mapper.readTree(patch), modelInputPatched);

        assertNull(modelInputPatched.get("freezeDeparturesBeforeTime"));
    }
}
