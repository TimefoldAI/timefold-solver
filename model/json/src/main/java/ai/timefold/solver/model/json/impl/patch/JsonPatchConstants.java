package ai.timefold.solver.model.json.impl.patch;

import java.util.regex.Pattern;

public final class JsonPatchConstants {

    public static Pattern HAS_EXPRESSION_PATTERN = Pattern.compile("\\[.*?\\]");
    public static Pattern EXTRACT_PATH_ELEMENTS_PATTERN = Pattern.compile("/[^/\\[]+|\\[.*?\\]");

    private JsonPatchConstants() {
        throw new UnsupportedOperationException();
    }
}
