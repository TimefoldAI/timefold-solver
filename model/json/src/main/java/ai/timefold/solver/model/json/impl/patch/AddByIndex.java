package ai.timefold.solver.model.json.impl.patch;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AddByIndex implements Function<JsonNode, JsonNode> {

    private final String key;

    private final JsonNode value;

    public AddByIndex(String key, JsonNode value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public JsonNode apply(JsonNode parent) {
        // adding to an object
        if (parent.isObject()) {
            ObjectNode parentObject = (ObjectNode) parent;

            parentObject.set(key, value);
        }

        // adding to an array
        else if (parent.isArray()) {

            ArrayNode parentArray = (ArrayNode) parent;
            if (key.equals("-")) {
                parentArray.add(value);
            } else {
                try {
                    int idx = Integer.parseInt(key);
                    if (idx > parentArray.size() || idx < 0) {
                        throw new IllegalArgumentException("Array index is out of bounds: " + idx);
                    }
                    parentArray.insert(idx, value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid array index: " + key);
                }
            }
        }

        return parent;
    }

}
