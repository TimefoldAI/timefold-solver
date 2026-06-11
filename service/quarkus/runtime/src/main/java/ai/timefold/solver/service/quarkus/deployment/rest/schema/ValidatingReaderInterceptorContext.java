package ai.timefold.solver.service.quarkus.deployment.rest.schema;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import ai.timefold.solver.service.definition.api.ModelDescriptor;
import ai.timefold.solver.service.definition.api.validation.Validated;
import ai.timefold.solver.service.definition.api.validation.Validator;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.arc.All;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Special interceptor that allows to perform JSON schema validation for defined payloads.
 * It only triggers for dev and test profile as it requires double parsing of the payload
 * - first to ObjectNode for schema validation
 * - second for jackson object mapping
 * Returns bad request/400 with any validation errors found
 */
@IfBuildProfile(anyOf = { "dev", "test" })
@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class ValidatingReaderInterceptorContext implements ReaderInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatingReaderInterceptorContext.class);

    private final List<Validator<ObjectNode>> validators;
    private final ObjectMapper mapper;

    private String model;
    private String modelVersion;

    private boolean enabled;

    @Inject
    public ValidatingReaderInterceptorContext(@All List<Validator<ObjectNode>> validators, ObjectMapper mapper,
            @ConfigProperty(name = "ai.timefold.platform.models.validation.enable", defaultValue = "true") boolean enabled) {
        this.validators = validators;
        this.mapper = mapper;

        if (enabled) {

            try (InputStream in =
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("/timefold-model-descriptor.json")) {
                if (in != null) {
                    ModelDescriptor descriptor = mapper.readValue(in, ModelDescriptor.class);
                    this.model = descriptor.getModel();
                    this.modelVersion = descriptor.getVersion();
                    this.enabled = true;
                }
            } catch (IOException e) {
                LOGGER.error("Unable to read model descriptor due to {}, request payload will be disabled", e.getMessage(), e);
                this.enabled = false;
            }
        }

    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {

        if (enabled) {
            Optional<Annotation> validated = Stream.of(context.getAnnotations())
                    .filter(annotation -> annotation.annotationType() == Validated.class).findFirst();

            if (validated.isPresent()) {
                String operationId = ((Validated) validated.get()).operationId();
                boolean nullable = ((Validated) validated.get()).nullable();

                if (nullable && (context.getInputStream() == null || context.getInputStream().available() == 0)) {
                    LOGGER.debug("Operation {} allows null body, skipping validation", operationId);
                } else {

                    try (ByteArrayOutputStream dataHolder = new ByteArrayOutputStream()) {
                        context.getInputStream().transferTo(dataHolder);
                        byte[] data = dataHolder.toByteArray();

                        ObjectNode entity = mapper.readValue(data, ObjectNode.class);
                        List<String> allErrors = new ArrayList<>();

                        if (nullable && entity.isNull()) {
                            LOGGER.debug("Operation {} allows null body, skipping validation", operationId);
                        } else {

                            for (Validator<ObjectNode> validator : validators) {
                                List<String> errors = validator.validate(null, model, modelVersion, operationId, null, entity);
                                if (!errors.isEmpty()) {
                                    allErrors.addAll(errors);
                                }
                            }

                            if (!allErrors.isEmpty()) {
                                throw new BadRequestException(
                                        Response.status(Response.Status.BAD_REQUEST).entity(allErrors).build());
                            }
                        }

                        context.setInputStream(new ByteArrayInputStream(data));
                    } catch (BadRequestException e) {
                        throw e;
                    } catch (IllegalStateException e) {
                        throw new BadRequestException(
                                Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", e.getMessage())).build());
                    } catch (Exception e) {
                        throw new InternalServerErrorException(e);
                    }
                }
            }
        }
        return context.proceed();
    }

}
