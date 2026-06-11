package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.service.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;
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
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.ObjectParameter;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataModelConvertor;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class ModelConfigDescriptorDefaultTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataModelConfigOverrides.class, ObjectParameter.class, TestdataEntity.class,
                            TestdataSolution.class, TestdataConstraintProvider.class, TestdataModelConvertor.class,
                            TestdataRest.class))
            .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void detectModelConfigOverrides() throws IOException {
        File descriptor = new File("target/timefold/timefold-model-descriptor.json");

        ModelDescriptor modelDescriptor = objectMapper.readValue(descriptor, ModelDescriptor.class);
        ModelConfigDescriptor modelConfigDescriptor = modelDescriptor.getModelConfigDescriptor();

        assertThat(modelConfigDescriptor.schemaTypeRef()).isEqualTo(TestdataModelConfigOverrides.class.getSimpleName());

        assertThat(modelConfigDescriptor.configParameters()).hasSize(10);
        SoftAssertions.assertSoftly(softly -> {
            ModelConfigParameter constraintWeight = modelConfigDescriptor.configParameters().get(0);
            softly.assertThat(constraintWeight.id()).isEqualTo("constraintWeight");
            softly.assertThat(constraintWeight.name()).isEqualTo("constraintWeight");
            softly.assertThat(constraintWeight.description()).isNull();
            softly.assertThat(constraintWeight.kind()).isEqualTo(ParameterKind.WEIGHT);
            softly.assertThat(constraintWeight.type()).isEqualTo(Schema.SchemaType.INTEGER);

            ModelConfigParameter primitive = modelConfigDescriptor.configParameters().get(1);
            softly.assertThat(primitive.id()).isEqualTo("primitive");
            softly.assertThat(primitive.name()).isEqualTo("primitive");
            softly.assertThat(primitive.description()).isNull();
            softly.assertThat(primitive.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(primitive.type()).isEqualTo(Schema.SchemaType.INTEGER);

            ModelConfigParameter primitiveArray = modelConfigDescriptor.configParameters().get(2);
            softly.assertThat(primitiveArray.id()).isEqualTo("primitiveArray");
            softly.assertThat(primitiveArray.name()).isEqualTo("primitiveArray");
            softly.assertThat(primitiveArray.description()).isNull();
            softly.assertThat(primitiveArray.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(primitiveArray.type()).isEqualTo(Schema.SchemaType.ARRAY);
            softly.assertThat(primitiveArray.arrayItemType()).isEqualTo(Schema.SchemaType.INTEGER);
            softly.assertThat(primitiveArray.schemaTypeRef()).isNull();

            ModelConfigParameter duration = modelConfigDescriptor.configParameters().get(3);
            softly.assertThat(duration.id()).isEqualTo("duration");
            softly.assertThat(duration.name()).isEqualTo("duration");
            softly.assertThat(duration.description()).isNull();
            softly.assertThat(duration.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(duration.type()).isEqualTo(Schema.SchemaType.STRING);
            softly.assertThat(duration.format()).isEqualTo(DataFormat.Duration);
            softly.assertThat(duration.schemaTypeRef()).isEqualTo(Duration.class.getSimpleName());

            ModelConfigParameter string = modelConfigDescriptor.configParameters().get(4);
            softly.assertThat(string.id()).isEqualTo("string");
            softly.assertThat(string.name()).isEqualTo("string");
            softly.assertThat(string.description()).isNull();
            softly.assertThat(string.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(string.type()).isEqualTo(Schema.SchemaType.STRING);
            softly.assertThat(string.schemaTypeRef()).isNull();

            ModelConfigParameter stringList = modelConfigDescriptor.configParameters().get(5);
            softly.assertThat(stringList.id()).isEqualTo("stringList");
            softly.assertThat(stringList.name()).isEqualTo("stringList");
            softly.assertThat(stringList.description()).isNull();
            softly.assertThat(stringList.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(stringList.type()).isEqualTo(Schema.SchemaType.ARRAY);
            softly.assertThat(stringList.arrayItemType()).isEqualTo(Schema.SchemaType.STRING);
            softly.assertThat(stringList.schemaTypeRef()).isNull();

            ModelConfigParameter stringArray = modelConfigDescriptor.configParameters().get(6);
            softly.assertThat(stringArray.id()).isEqualTo("stringArray");
            softly.assertThat(stringArray.name()).isEqualTo("stringArray");
            softly.assertThat(stringArray.description()).isNull();
            softly.assertThat(stringArray.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(stringArray.type()).isEqualTo(Schema.SchemaType.ARRAY);
            softly.assertThat(stringArray.arrayItemType()).isEqualTo(Schema.SchemaType.STRING);
            softly.assertThat(stringArray.schemaTypeRef()).isNull();

            ModelConfigParameter object = modelConfigDescriptor.configParameters().get(7);
            softly.assertThat(object.id()).isEqualTo("object");
            softly.assertThat(object.name()).isEqualTo("object");
            softly.assertThat(object.description()).isNull();
            softly.assertThat(object.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(object.type()).isEqualTo(Schema.SchemaType.OBJECT);
            softly.assertThat(object.schemaTypeRef()).isEqualTo(ObjectParameter.class.getSimpleName());

            ModelConfigParameter objectList = modelConfigDescriptor.configParameters().get(8);
            softly.assertThat(objectList.id()).isEqualTo("objectList");
            softly.assertThat(objectList.name()).isEqualTo("objectList");
            softly.assertThat(objectList.description()).isNull();
            softly.assertThat(objectList.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(objectList.type()).isEqualTo(Schema.SchemaType.ARRAY);
            softly.assertThat(objectList.arrayItemType()).isEqualTo(Schema.SchemaType.OBJECT);
            softly.assertThat(objectList.schemaTypeRef()).isEqualTo(ObjectParameter.class.getSimpleName());

            ModelConfigParameter objectArray = modelConfigDescriptor.configParameters().get(9);
            softly.assertThat(objectArray.id()).isEqualTo("objectArray");
            softly.assertThat(objectArray.name()).isEqualTo("objectArray");
            softly.assertThat(objectArray.description()).isNull();
            softly.assertThat(objectArray.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(objectArray.type()).isEqualTo(Schema.SchemaType.ARRAY);
            softly.assertThat(objectArray.arrayItemType()).isEqualTo(Schema.SchemaType.OBJECT);
            softly.assertThat(objectArray.schemaTypeRef()).isEqualTo(ObjectParameter.class.getSimpleName());
        });
    }
}
