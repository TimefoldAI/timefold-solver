package ai.timefold.solver.model.rest.impl.error;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/**
 * Extended and aligned version of io.quarkus.resteasy.reactive.jackson.runtime.mappers.DefaultMismatchedInputException that
 * only reports errors for dev and test mode and in prod mode returns empty body leaving users without any information what
 * was wrong with the submitted request.
 */
@Provider
public class JacksonMismatchedInputExceptionMapper
        implements ExceptionMapper<MismatchedInputException> {

    private static final Pattern CLEANUP_PACKAGE_PATTERN = Pattern
            .compile("(?:([a-zA-Z_$][a-zA-Z\\d_$]*(?:\\.[a-zA-Z_$][a-zA-Z\\d_$]*)*)\\.)?([a-zA-Z_$][a-zA-Z\\d_$]*)");

    @Override
    public Response toResponse(MismatchedInputException exception) {
        var responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        List<String> issues = new ArrayList<>();
        List<JsonMappingException.Reference> path = exception.getPath();
        if (path != null && !path.isEmpty()) {
            StringBuilder attributeNameBuilder = new StringBuilder();

            for (JsonMappingException.Reference pathReference : path) {
                if (pathReference.getFieldName() != null) {
                    if (attributeNameBuilder.length() > 0) {
                        attributeNameBuilder.append(".");
                    }

                    attributeNameBuilder.append(pathReference.getFieldName());
                }

                if (pathReference.getIndex() >= 0) {
                    attributeNameBuilder.append("[");
                    attributeNameBuilder.append(pathReference.getIndex());
                    attributeNameBuilder.append("]");
                }
            }

            if (attributeNameBuilder.length() > 0) {
                issues.add(attributeNameBuilder.toString() + ": " + removeInternalPackageNames(exception.getOriginalMessage()));
            }
        } else {
            issues.add(exception.getPathReference() + ": " + removeInternalPackageNames(exception.getOriginalMessage()));
        }

        return responseBuilder.entity(issues).build();
    }

    /**
     * Removes package names from the given text as Jackson provides fully qualified names of classes that were involved in the
     * parsing issue.
     *
     * @param text Jackson error message
     * @return cleaned up (removed package names) error message
     */
    private String removeInternalPackageNames(String text) {
        Matcher matcher = CLEANUP_PACKAGE_PATTERN.matcher(text);

        while (matcher.find()) {
            String pkg = matcher.group(1);
            if (pkg != null) {
                text = text.replace(pkg + ".", "");
            }
        }
        return text;
    }
}
