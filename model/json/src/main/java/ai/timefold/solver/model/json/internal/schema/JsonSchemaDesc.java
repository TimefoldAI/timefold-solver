package ai.timefold.solver.model.json.internal.schema;

import java.io.InputStream;
import java.util.Set;

public record JsonSchemaDesc(InputStream content, Set<String> registersWith) {

}
