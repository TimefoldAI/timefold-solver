package ai.timefold.solver.service.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.ModelDescriptor;
import ai.timefold.solver.service.definition.api.domain.DataFormat;
import ai.timefold.solver.service.definition.internal.descriptor.ModelConfigDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ModelConfigParameter;
import ai.timefold.solver.service.definition.internal.descriptor.ParameterKind;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.ObjectParameter;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataModelConvertor;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusExtensionTest;

public class ModelConfigDescriptorSchemaTest {

    @RegisterExtension
    static final QuarkusExtensionTest config =
            ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataModelConfigOverrides.class,
                    ObjectParameter.class, TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class,
                    TestdataModelConvertor.class, TestdataRest.class);

    @Inject
    ObjectMapper objectMapper;

    @Test
    void detectModelConfigOverrides() throws IOException {
        File descriptor = new File("target/timefold/timefold-model-descriptor.json");

        ModelDescriptor modelDescriptor = objectMapper.readValue(descriptor, ModelDescriptor.class);
        ModelConfigDescriptor modelConfigDescriptor = modelDescriptor.getModelConfigDescriptor();

        assertThat(modelConfigDescriptor.schemaTypeRef()).isEqualTo(TestdataModelConfigOverrides.class.getSimpleName());

        assertThat(modelConfigDescriptor.configParameters()).hasSize(5);
        SoftAssertions.assertSoftly(softly -> {
            ModelConfigParameter constraintWeight = modelConfigDescriptor.configParameters().get(0);
            softly.assertThat(constraintWeight.id()).isEqualTo("constraintWeight");
            softly.assertThat(constraintWeight.name()).isEqualTo("Constraint weight");
            softly.assertThat(constraintWeight.description()).isEqualTo("Constraint weight description");
            softly.assertThat(constraintWeight.kind()).isEqualTo(ParameterKind.WEIGHT);
            softly.assertThat(constraintWeight.type()).isEqualTo(Schema.SchemaType.INTEGER);

            ModelConfigParameter primitive = modelConfigDescriptor.configParameters().get(1);
            softly.assertThat(primitive.id()).isEqualTo("primitiveParam");
            softly.assertThat(primitive.name()).isEqualTo("primitiveParam");
            softly.assertThat(primitive.description()).isNull();
            softly.assertThat(primitive.format()).isEqualTo(DataFormat.Percentage);
            softly.assertThat(primitive.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(primitive.type()).isEqualTo(Schema.SchemaType.INTEGER);
            softly.assertThat(primitive.nullable()).isFalse();

            ModelConfigParameter primitiveArray = modelConfigDescriptor.configParameters().get(2);
            softly.assertThat(primitiveArray.id()).isEqualTo("primitiveArrayParam");
            softly.assertThat(primitiveArray.name()).isEqualTo("Array of numbers");
            softly.assertThat(primitiveArray.description()).isNull();
            softly.assertThat(primitiveArray.format()).isEqualTo(DataFormat.Number);
            softly.assertThat(primitiveArray.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(primitiveArray.type()).isEqualTo(Schema.SchemaType.ARRAY);
            softly.assertThat(primitiveArray.schemaTypeRef()).isNull();

            ModelConfigParameter duration = modelConfigDescriptor.configParameters().get(3);
            softly.assertThat(duration.id()).isEqualTo("duration");
            softly.assertThat(duration.name()).isEqualTo("Duration");
            softly.assertThat(duration.description()).isNull();
            softly.assertThat(duration.format()).isEqualTo(DataFormat.Duration);
            softly.assertThat(duration.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(duration.type()).isEqualTo(Schema.SchemaType.STRING);
            softly.assertThat(duration.schemaTypeRef()).isEqualTo(Duration.class.getSimpleName());

            ModelConfigParameter string = modelConfigDescriptor.configParameters().get(4);
            softly.assertThat(string.id()).isEqualTo("string");
            softly.assertThat(string.name()).isEqualTo("string");
            softly.assertThat(string.description()).isEqualTo("String parameter");
            softly.assertThat(string.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(string.type()).isEqualTo(Schema.SchemaType.STRING);
            softly.assertThat(string.schemaTypeRef()).isNull();
            softly.assertThat(string.nullable()).isTrue();
        });
    }
}
