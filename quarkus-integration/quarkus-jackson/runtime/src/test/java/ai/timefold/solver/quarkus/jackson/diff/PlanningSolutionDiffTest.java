package ai.timefold.solver.quarkus.jackson.diff;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.testdomain.equals.TestdataEqualsByCodeSolution;
import ai.timefold.solver.quarkus.jackson.TimefoldJacksonModule;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

class PlanningSolutionDiffTest {

    @Test
    void serialize() throws JsonProcessingException {
        var oldSolution = TestdataEqualsByCodeSolution.generateSolution("A", 2, 2);
        oldSolution.getEntityList().get(0).setValue(oldSolution.getValueList().get(1));
        var newSolution = TestdataEqualsByCodeSolution.generateSolution("B", 3, 3);
        var solutionDescriptor = TestdataEqualsByCodeSolution.buildSolutionDescriptor();
        var diff = solutionDescriptor.diff(oldSolution, newSolution);

        var objectMapper = JsonMapper.builder()
                .addModule(TimefoldJacksonModule.createModule())
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        var serialized = objectMapper.writeValueAsString(diff);
        assertThat(serialized)
                .isEqualToIgnoringWhitespace("""
                        {
                           "added_entities": [
                             {
                               "code": "Generated Entity 2",
                               "value": {
                                 "code": "Generated Value 2"
                               }
                             }
                           ],
                           "entity_diffs": [
                             {
                               "entity": {
                                 "code": "Generated Entity 0",
                                 "value": {
                                   "code": "Generated Value 1"
                                 }
                               },
                               "entity_class": "ai.timefold.solver.core.testdomain.equals.TestdataEqualsByCodeEntity",
                               "variable_diffs": [
                                 {
                                   "name": "value",
                                   "new_value": {
                                     "code": "Generated Value 0"
                                   },
                                   "old_value": {
                                     "code": "Generated Value 1"
                                   }
                                 }
                               ]
                             }
                           ],
                           "removed_entities": []
                         }""");

    }

}
