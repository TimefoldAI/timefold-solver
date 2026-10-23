package ai.timefold.solver.service.definition.api.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class UniqueResourcesTest {

    @Test
    void fromSingleResourceType() {
        var uniqueResources = UniqueResources.from("employee", List.of("Ann", "Beth", "Ann"));

        assertThat(uniqueResources.uniqueResourceIdsPerType()).containsOnlyKeys("employee");
        assertThat(uniqueResources.uniqueResourceIdsPerType().get("employee")).containsExactly("Ann", "Beth");
    }

    @Test
    void fromRejectsNulls() {
        assertThatThrownBy(() -> UniqueResources.from(null, List.of("Ann")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UniqueResources.from("employee", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorDeduplicatesAndPreservesOrder() {
        Map<String, Collection<String>> map = new LinkedHashMap<>();
        map.put("vehicle", List.of("V2", "V1", "V2"));
        map.put("employee", List.of("Beth", "Ann", "Beth"));

        var uniqueResources = new UniqueResources(map);

        assertThat(uniqueResources.uniqueResourceIdsPerType()).containsOnlyKeys("vehicle", "employee");
        assertThat(uniqueResources.uniqueResourceIdsPerType().keySet()).containsExactly("vehicle", "employee");
        assertThat(uniqueResources.uniqueResourceIdsPerType().get("vehicle")).containsExactly("V2", "V1");
        assertThat(uniqueResources.uniqueResourceIdsPerType().get("employee")).containsExactly("Beth", "Ann");
    }

    @Test
    void constructorCopiesInput() {
        var employeeIds = new ArrayList<>(List.of("Ann"));
        Map<String, Collection<String>> map = new HashMap<>();
        map.put("employee", employeeIds);

        var uniqueResources = new UniqueResources(map);
        employeeIds.add("Beth");
        map.put("vehicle", List.of("V1"));

        assertThat(uniqueResources.uniqueResourceIdsPerType()).containsOnlyKeys("employee");
        assertThat(uniqueResources.uniqueResourceIdsPerType().get("employee")).containsExactly("Ann");
    }

    @Test
    void resultIsUnmodifiable() {
        var uniqueResources = UniqueResources.from("employee", List.of("Ann"));
        var uniqueResourceIdsPerType = uniqueResources.uniqueResourceIdsPerType();
        var employeeIds = uniqueResourceIdsPerType.get("employee");

        assertThatThrownBy(() -> uniqueResourceIdsPerType.put("vehicle", List.of("V1")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> employeeIds.add("Beth"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(employeeIds::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructorRejectsNulls() {
        assertThatThrownBy(() -> new UniqueResources(null))
                .isInstanceOf(NullPointerException.class);

        Map<String, Collection<String>> nullKey = new HashMap<>();
        nullKey.put(null, List.of("Ann"));
        assertThatThrownBy(() -> new UniqueResources(nullKey))
                .isInstanceOf(NullPointerException.class);

        Map<String, Collection<String>> nullValue = new HashMap<>();
        nullValue.put("employee", null);
        assertThatThrownBy(() -> new UniqueResources(nullValue))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalRegardlessOfInputCollectionType() {
        var fromList = UniqueResources.from("employee", List.of("Ann", "Beth"));
        var fromBuilder = UniqueResources.builder()
                .addResourceType("employee", List.of("Ann", "Beth"))
                .build();

        assertThat(fromList).isEqualTo(fromBuilder);
        assertThat(fromList.hashCode()).isEqualTo(fromBuilder.hashCode());
    }

    @Test
    void builderWithMultipleResourceTypes() {
        var uniqueResources = UniqueResources.builder()
                .addResourceType("vehicle", List.of("V1", "V2"))
                .addResourceType("employee", List.of("Ann"))
                .build();

        assertThat(uniqueResources.uniqueResourceIdsPerType().keySet()).containsExactly("vehicle", "employee");
        assertThat(uniqueResources.uniqueResourceIdsPerType().get("vehicle")).containsExactly("V1", "V2");
        assertThat(uniqueResources.uniqueResourceIdsPerType().get("employee")).containsExactly("Ann");
    }

    @Test
    void builderMergesSameResourceType() {
        var uniqueResources = UniqueResources.builder()
                .addResourceType("employee", List.of("Ann", "Beth"))
                .addResourceType("vehicle", List.of("V1"))
                .addResourceType("employee", List.of("Beth", "Cecil"))
                .build();

        assertThat(uniqueResources.uniqueResourceIdsPerType().keySet()).containsExactly("employee", "vehicle");
        assertThat(uniqueResources.uniqueResourceIdsPerType().get("employee")).containsExactly("Ann", "Beth", "Cecil");
    }

    @Test
    void builderWithoutResourceTypes() {
        var uniqueResources = UniqueResources.builder().build();

        assertThat(uniqueResources.uniqueResourceIdsPerType()).isEmpty();
    }

    @Test
    void builderCopiesInput() {
        var employeeIds = new ArrayList<>(List.of("Ann"));
        var builder = UniqueResources.builder().addResourceType("employee", employeeIds);
        employeeIds.add("Beth");

        assertThat(builder.build().uniqueResourceIdsPerType().get("employee")).containsExactly("Ann");
    }

    @Test
    void builtResultIsIndependentOfBuilder() {
        var builder = UniqueResources.builder().addResourceType("employee", List.of("Ann"));
        var first = builder.build();
        builder.addResourceType("employee", List.of("Beth"))
                .addResourceType("vehicle", List.of("V1"));
        var second = builder.build();

        assertThat(first.uniqueResourceIdsPerType()).containsOnlyKeys("employee");
        assertThat(first.uniqueResourceIdsPerType().get("employee")).containsExactly("Ann");
        assertThat(second.uniqueResourceIdsPerType().keySet()).containsExactly("employee", "vehicle");
        assertThat(second.uniqueResourceIdsPerType().get("employee")).containsExactly("Ann", "Beth");
    }

    @Test
    void builderRejectsNulls() {
        var builder = UniqueResources.builder();

        assertThatThrownBy(() -> builder.addResourceType(null, List.of("Ann")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.addResourceType("employee", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void jsonRoundTrip() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var original = UniqueResources.builder()
                .addResourceType("vehicle", List.of("V1", "V2"))
                .addResourceType("employee", List.of("Ann"))
                .build();

        var json = mapper.writeValueAsString(original);
        var deserialized = mapper.readValue(json, UniqueResources.class);

        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.uniqueResourceIdsPerType().keySet()).containsExactly("vehicle", "employee");
    }

    @Test
    void jsonDeserializationDeduplicates() throws JsonProcessingException {
        var deserialized = new ObjectMapper().readValue("""
                {"uniqueResourceIdsPerType": {"employee": ["Ann", "Ann", "Beth"]}}
                """, UniqueResources.class);

        assertThat(deserialized.uniqueResourceIdsPerType().get("employee")).containsExactly("Ann", "Beth");
        assertThat(deserialized).isEqualTo(UniqueResources.from("employee", List.of("Ann", "Beth")));
    }
}
