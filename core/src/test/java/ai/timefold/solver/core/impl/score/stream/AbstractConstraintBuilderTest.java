package ai.timefold.solver.core.impl.score.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public abstract class AbstractConstraintBuilderTest {

    protected abstract AbstractConstraintBuilder<SimpleScore> of(String constraintId);

    @CsvSource("""
            name, true
            Name and spaces, true
            Name (or spaces), true
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
    void idSanitized(String id, boolean correct) {
        if (correct) {
            var constraint = of(id)
                    .asConstraint(id);
            assertThat(constraint)
                    .isNotNull();
        } else {
            assertThatThrownBy(() -> of(id).asConstraint(id))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

}
