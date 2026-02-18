package ai.timefold.solver.core.impl.score.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public abstract class AbstractConstraintBuilderTest {

    protected abstract AbstractConstraintBuilder<SimpleScore> of(String constraintName, String constraintGroup);

    @CsvSource("""
            name, true
            Name and spaces, true
            Name and numb3rs, true
            name_and_numb3r5, true
            name-and-numb3r5, true
            Let's allow a complete sentence., true
            name${something}name, false
            name$1name, false
            name%23name, false
            name/name, false
            name//name, false
            name\\/name, false
            name\\/\\/name, false
            null, false
            nil, false
             , false
            -, false
            _, false""")
    @ParameterizedTest
    void nameSanitized(String name, boolean correct) {
        if (correct) {
            var constraint = of(name, "group")
                    .asConstraintDescribed(name, UUID.randomUUID().toString());
            assertThat(constraint)
                    .isNotNull();
        } else {
            assertThatThrownBy(() -> of(name, "group").asConstraintDescribed(name, UUID.randomUUID().toString()))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    @CsvSource("""
            name, true
            Name and spaces, true
            Name and numb3rs, true
            name_and_numb3r5, true
            name-and-numb3r5, true
            Let's allow a complete sentence., true
            name${something}name, false
            name$1name, false
            name%23name, false
            name/name, false
            name//name, false
            name\\/name, false
            name\\/\\/name, false
            null, false
            nil, false
             , false
            -, false
            _, false""")
    @ParameterizedTest
    void groupSanitized(String group, boolean correct) {
        if (correct) {
            var constraint = of("name", group)
                    .asConstraintDescribed(group, UUID.randomUUID().toString());
            assertThat(constraint)
                    .isNotNull();
        } else {
            assertThatThrownBy(() -> of("name", group).asConstraintDescribed(group, UUID.randomUUID().toString()))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

}
