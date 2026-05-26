package ai.timefold.solver.model.json.internal.patch;

import java.util.function.Function;
import java.util.regex.Matcher;

import ai.timefold.solver.model.definition.api.ModelInputPatchOp;
import ai.timefold.solver.model.json.impl.patch.AddByFilter;
import ai.timefold.solver.model.json.impl.patch.AddByIndex;
import ai.timefold.solver.model.json.impl.patch.JsonPatchConstants;
import ai.timefold.solver.model.json.impl.patch.RemoveByFilter;
import ai.timefold.solver.model.json.impl.patch.RemoveByIndex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonPatch {

    private JsonPatch() {
        // utility class should not be instantiated
    }

    /**
     * Applies all JSON patch operations to a JSON document.
     *
     * @return the patched JSON document
     */
    public static JsonNode apply(ArrayNode patch, JsonNode source) {
        if (!source.isContainerNode()) {
            throw new IllegalArgumentException("Invalid JSON document, "
                    + "an object or array is required");
        }

        if (patch.size() == 0) {
            return source;
        }

        for (JsonNode operation : patch) {
            if (!operation.isObject()) {
                throw new IllegalArgumentException("Invalid operation: " + operation);
            }
            source = perform((ObjectNode) operation, source);
        }

        return source;
    }

    /**
     * Perform one JSON patch operation
     *
     * @return the patched JSON document
     */
    protected static JsonNode perform(ObjectNode operation, JsonNode doc) {
        JsonNode opNode = operation.get("op");
        if (opNode == null || !opNode.isTextual()) {
            throw new IllegalArgumentException("Invalid \"op\" property: " + opNode);
        }
        String op = opNode.asText();
        JsonNode pathNode = operation.get("path");
        if (pathNode == null || !pathNode.isTextual()) {
            throw new IllegalArgumentException("Invalid \"path\" property: " + pathNode);
        }
        String path = pathNode.asText();
        if (path.length() != 0 && path.charAt(0) != '/') {
            throw new IllegalArgumentException("Invalid \"path\" property: " + path);
        }

        ModelInputPatchOp patchOperation = ModelInputPatchOp.valueOf(op);

        switch (patchOperation) {

            case add: {
                JsonNode value = operation.get("value");
                if (value == null) {
                    throw new IllegalArgumentException("Missing \"value\" property");
                }
                return add(doc, path, value);
            }

            case remove: {
                return remove(doc, path);
            }

            case replace: {
                JsonNode value = operation.get("value");
                if (value == null) {
                    throw new IllegalArgumentException("Missing \"value\" property");
                }
                return replace(doc, path, value);
            }

            default:
                throw new IllegalArgumentException("Invalid \"op\" property: " + op);
        }
    }

    /**
     * Perform a JSON patch "add" operation on a JSON document
     *
     * @return the patched JSON document
     */
    protected static JsonNode add(JsonNode doc, String path, JsonNode value) {
        if (path.isEmpty()) {
            return value;
        }

        // get the path parent
        JsonNode parent = null;
        int lastPathIndex = path.lastIndexOf('/');
        if (lastPathIndex == 0) {
            String fieldName = path.substring(lastPathIndex + 1);
            if (doc.isObject()) {
                ((ObjectNode) doc).set(fieldName, value);
            }
        } else if (lastPathIndex < 1) {
            parent = doc;
        } else {

            Function<JsonNode, JsonNode> performer = new AddByIndex(path.substring(lastPathIndex + 1), value);

            // check if there is expression (form at of expression is [field=value]
            Matcher expressionMatcher = JsonPatchConstants.HAS_EXPRESSION_PATTERN.matcher(path);
            if (expressionMatcher.find()) {
                Matcher matcher = JsonPatchConstants.EXTRACT_PATH_ELEMENTS_PATTERN.matcher(path);
                StringBuilder suffixPath = null;
                // iterate over all fragments of the path
                while (matcher.find()) {
                    String fragment = matcher.group();

                    if (suffixPath != null) {
                        suffixPath.append(fragment.startsWith("/") ? fragment : "/" + fragment);
                        continue;
                    }

                    // when fragment is expression, apply the actual expression performer
                    if (fragment.startsWith("[") && fragment.endsWith("]")) {
                        suffixPath = new StringBuilder();
                        performer = new AddByFilter(fragment, value, suffixPath);

                    } else {
                        // regular path as json pointer so find the parent that expression will be applied on
                        parent = doc.at(fragment);
                    }

                }
            } else {
                // no expression in the path, process as json pointer to find the parent
                parent = doc.at(path.substring(0, lastPathIndex));
            }

            if (parent.isMissingNode()) {
                throw new IllegalArgumentException("Path does not exist: " + path);
            }
            performer.apply(parent);

            String parentPath = path.substring(0, lastPathIndex);
            parent = doc.at(parentPath);
        }

        return doc;
    }

    /**
     * Perform a JSON patch "remove" operation on a JSON document
     *
     * @return the patched JSON document
     */
    protected static JsonNode remove(JsonNode doc, String path) {
        if (path.equals("")) {
            if (doc.isObject()) {
                ObjectNode docObject = (ObjectNode) doc;
                docObject.removeAll();
                return doc;
            } else if (doc.isArray()) {
                ArrayNode docArray = (ArrayNode) doc;
                docArray.removeAll();
                return doc;
            }
        }

        // get the path parent either by index (json pointer) or by filter
        JsonNode parent = null;
        int lastPathIndex = path.lastIndexOf('/');
        Function<JsonNode, JsonNode> performer = new RemoveByIndex(path.substring(lastPathIndex + 1));

        if (lastPathIndex == 0) {
            parent = doc;
        } else {
            // check if there is expression (form at of expression is [field=value]
            Matcher expressionMatcher = JsonPatchConstants.HAS_EXPRESSION_PATTERN.matcher(path);
            if (expressionMatcher.find()) {
                Matcher matcher = JsonPatchConstants.EXTRACT_PATH_ELEMENTS_PATTERN.matcher(path);
                StringBuilder suffixPath = null;
                // iterate over all fragments of the path
                while (matcher.find()) {
                    String fragment = matcher.group();

                    if (suffixPath != null) {
                        suffixPath.append(fragment.startsWith("/") ? fragment : "/" + fragment);
                        continue;
                    }

                    // when fragment is expression, apply the actual expression performer
                    if (fragment.startsWith("[") && fragment.endsWith("]")) {
                        suffixPath = new StringBuilder();
                        performer = new RemoveByFilter(fragment, suffixPath);

                    } else {
                        // regular path as json pointer so find the parent that expression will be applied on
                        parent = doc.at(fragment);
                    }

                }
            } else {
                // no expression in the path, process as json pointer to find the parent
                parent = doc.at(path.substring(0, lastPathIndex));
            }

        }
        if (parent.isMissingNode()) {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }
        performer.apply(parent);

        return doc;
    }

    /**
     * Perform a JSON patch "replace" operation on a JSON document
     *
     * @return the patched JSON document
     */
    protected static JsonNode replace(JsonNode doc, String path, JsonNode value) {
        doc = remove(doc, path);
        return add(doc, path, value);
    }

}
