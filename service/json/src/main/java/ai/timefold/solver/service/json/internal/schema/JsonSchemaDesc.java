package ai.timefold.solver.service.json.internal.schema;

import java.io.InputStream;
import java.util.Set;

public record JsonSchemaDesc(InputStream content, Set<String> registersWith) {

}
