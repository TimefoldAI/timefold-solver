package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.CustomShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.domain.InvalidFieldTestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.domain.InvalidMethodTestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.domain.InvalidEntitySpringTestConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
class IncludeAbstractClassesEntityScannerTest {

    private static final Class<? extends Annotation>[] PLANNING_ENTITY_FIELD_ANNOTATIONS = new Class[] {
            PlanningPin.class,
            PlanningVariable.class,
            PlanningListVariable.class,
            AnchorShadowVariable.class,
            CustomShadowVariable.class,
            IndexShadowVariable.class,
            InverseRelationShadowVariable.class,
            NextElementShadowVariable.class,
            PiggybackShadowVariable.class,
            PreviousElementShadowVariable.class,
            ShadowVariable.class
    };

    private final ApplicationContextRunner contextRunner;

    public IncludeAbstractClassesEntityScannerTest() {
        contextRunner = new ApplicationContextRunner().withUserConfiguration(InvalidEntitySpringTestConfiguration.class);
    }

    @Test
    void testInvalidProperties() {
        contextRunner
                .run(context -> {
                    IncludeAbstractClassesEntityScanner scanner = new IncludeAbstractClassesEntityScanner(context);

                    // Each field
                    Arrays.stream(PLANNING_ENTITY_FIELD_ANNOTATIONS).forEach(annotation -> {
                        List<Class<?>> classes = scanner.findAnnotationsWithRepeatable(annotation);
                        assertThat(classes).hasSize(2);
                        assertThat(classes).contains(InvalidFieldTestdataSpringEntity.class,
                                InvalidMethodTestdataSpringEntity.class);
                    });

                    // All fields
                    List<Class<?>> classes = scanner.findAnnotationsWithRepeatable(PLANNING_ENTITY_FIELD_ANNOTATIONS);
                    assertThat(classes).hasSize(2);
                    assertThat(classes).contains(InvalidFieldTestdataSpringEntity.class,
                            InvalidMethodTestdataSpringEntity.class);
                });
    }
}
