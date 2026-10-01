package ai.timefold.solver.service.definition.api;

import java.util.Collection;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.Schema.True;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

public record ModelInputPatch(@Schema(required = true, description = "Operation to be applied") ModelInputPatchOp op,
        @Schema(required = true,
                description = "Path within the dataset structure to be modified. Can use field expression with format [fieldName=value] or index to point to selected objects",
                examples = {
                        "/employees/[id=Employee123]", "/visits/[name=Important]", "/visits/2" }) String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(anyOf = { Object.class, Collection.class },
                additionalProperties = True.class,
                implementation = Object.class,
                description = "Value (object, array or simple value) to be used for add or replace operations. It should not be given for remove operation") JsonNode value) {

}
