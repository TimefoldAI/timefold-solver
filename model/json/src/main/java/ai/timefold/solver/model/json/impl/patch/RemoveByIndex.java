package ai.timefold.solver.model.json.impl.patch;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RemoveByIndex implements Function<JsonNode, JsonNode> {

    private final String key;

    public RemoveByIndex(String key) {
        this.key = key;
    }

    @Override
    public JsonNode apply(JsonNode parent) {
        // removing from an object
        if (parent.isObject()) {
            ObjectNode parentObject = (ObjectNode) parent;
            if (!parent.has(key)) {
                throw new IllegalArgumentException("Property does not exist: " + key);
            }
            parentObject.remove(key);
        }

        // removing from an array
        else if (parent.isArray()) {
            ArrayNode parentArray = (ArrayNode) parent;
            try {

                int idx = Integer.parseInt(key);
                if (!parent.has(idx)) {
                    throw new IllegalArgumentException("Index does not exist: " + key);
                }
                parentArray.remove(idx);
            } catch (NumberFormatException e) {
                // if given key is not an index, try remove by the value itself if is textual values in the array
                parentArray.removeIf(val -> val.isTextual() && val.asText().equals(key));
            }
        }
        return parent;
    }

}
