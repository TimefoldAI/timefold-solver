package ai.timefold.solver.model.json.impl.patch;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AddByFilter implements Function<JsonNode, JsonNode> {

    private final ExpressionHandler expression;

    private final JsonNode value;

    private StringBuilder suffixPath;

    public AddByFilter(String expression, JsonNode value, StringBuilder suffixPath) {
        this.expression = ExpressionHandler.from(expression);
        this.value = value;
        this.suffixPath = suffixPath;
    }

    @Override
    public JsonNode apply(JsonNode jsonNode) {

        // extract final part of the field to look up as in the expression it can be either field or path
        int lastPathIndexExp = expression.getFieldOrPath().lastIndexOf('/');

        if (jsonNode.isObject()) {

            processObject(jsonNode, lastPathIndexExp);
        } else if (jsonNode.isArray()) {

            processArray(jsonNode, lastPathIndexExp);
        }
        return jsonNode;
    }

    private void processObject(JsonNode jsonNode, int lastPathIndexExp) {

        JsonNode source;
        // removing from an object
        ObjectNode parentObject = (ObjectNode) jsonNode;
        // if the fields in the expression was just the field use the main jsonNode as source
        if (lastPathIndexExp == 0) {
            source = jsonNode;
        } else {
            // otherwise look up the source node by the expression's path
            source = jsonNode.at("/" + expression.getFieldOrPath());
        }
        if (expression.evaluate(source)) {
            // locate the final target field to be added to (by field name or from path)
            int lastPathIndexProp = suffixPath.toString().lastIndexOf('/');
            String propertyName = suffixPath.toString().substring(lastPathIndexProp + 1);
            JsonNode target = parentObject.at(suffixPath.toString().substring(0, lastPathIndexProp));

            // add the property directly to the object
            if (target.isObject()) {
                ((ObjectNode) target).set(propertyName, this.value);
            } else if (target.isArray()) {
                // or add property in every object of the array of objects
                target.forEach(item -> ((ObjectNode) item).set(propertyName, this.value));
            }

        }
    }

    private void processArray(JsonNode jsonNode, int lastPathIndexExp) {
        // removing from an array
        ArrayNode parentArray = (ArrayNode) jsonNode;
        // find the elements of the collection that match the condition
        List<JsonNode> items = parentArray.valueStream().filter(item -> {

            if (lastPathIndexExp > 0) {
                item = item.at("/" + expression.getFieldOrPath().substring(0, lastPathIndexExp));
            }
            if (item instanceof ObjectNode) {
                return expression.evaluate(item);
            } else if (item instanceof ArrayNode) {
                return item.valueStream().anyMatch(val -> expression.evaluate(val));
            }
            return false;
        }).toList();

        if (items.isEmpty() && suffixPath.isEmpty()) {
            // in case filter returns empty result consider it an add to parent e.g. replace operation with filter
            parentArray.add(this.value);
            return;
        } else {
            // for each item that matched the condition perform the addition
            items.forEach(item -> {

                // check if there is expression within the sub path and if so process recursively
                if (JsonPatchConstants.HAS_EXPRESSION_PATTERN.matcher(suffixPath).find()) {
                    Matcher matcher = JsonPatchConstants.EXTRACT_PATH_ELEMENTS_PATTERN.matcher(suffixPath);
                    StringBuilder suffixPath = null;
                    JsonNode parent = null;
                    Function<JsonNode, JsonNode> performer = null;
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
                            parent = item.at(fragment);
                        }

                    }
                    performer.apply(parent);
                } else {
                    // locate the final target field to be added at
                    int lastPathIndexProp = suffixPath.toString().lastIndexOf('/');
                    String propertyName = suffixPath.toString().substring(lastPathIndexProp + 1);
                    JsonNode target = item.at(suffixPath.toString().substring(0, lastPathIndexProp));

                    // if the final node is an object add to its fields
                    if (target.isObject()) {
                        ((ObjectNode) target).set(propertyName, this.value);
                    } else if (target.isArray()) {
                        // in case of an array `-` means append at the end
                        if (propertyName.equals("-")) {
                            ((ArrayNode) target).add(this.value);
                        } else {
                            // otherwise add field to each element of the array
                            ((ArrayNode) target).forEach(sitem -> ((ObjectNode) sitem).set(propertyName, this.value));
                        }
                    } else if (target.isMissingNode()) {
                        // in case of an array `-` means append at the end but since missing node add collection first
                        if (propertyName.equals("-")) {
                            ArrayNode newArray = ((ContainerNode<?>) jsonNode).arrayNode();
                            newArray.add(this.value);

                            String missingNodePropertyName = suffixPath.toString().substring(0, lastPathIndexProp);
                            int lastPathIndexPropMissingNode = missingNodePropertyName.lastIndexOf('/');
                            missingNodePropertyName = missingNodePropertyName.substring(lastPathIndexPropMissingNode + 1);

                            if (item.isObject()) {
                                ((ObjectNode) item).set(missingNodePropertyName, newArray);
                            }
                        }
                    }
                }
            });
        }
    }
}
