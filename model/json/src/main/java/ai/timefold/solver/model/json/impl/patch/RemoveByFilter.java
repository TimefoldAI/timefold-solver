package ai.timefold.solver.model.json.impl.patch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RemoveByFilter implements Function<JsonNode, JsonNode> {

    private final ExpressionHandler expression;

    private final StringBuilder suffixPath;

    public RemoveByFilter(String expression, StringBuilder suffixPath) {
        this.expression = ExpressionHandler.from(expression);
        this.suffixPath = suffixPath;
    }

    @Override
    public JsonNode apply(JsonNode jsonNode) {

        // extract final part of the field to look up as in the expression it can be either field or path
        int lastPathIndexExp = expression.getFieldOrPath().lastIndexOf('/');
        String key = expression.getFieldOrPath().substring(lastPathIndexExp + 1);

        if (jsonNode.isObject()) {

            processObject(jsonNode, lastPathIndexExp, key);
        } else if (jsonNode.isArray()) {

            processArray(jsonNode, lastPathIndexExp, key);
        }
        return jsonNode;

    }

    private void processObject(JsonNode jsonNode, int lastPathIndexExp, String key) {
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
        // evaluate expression to see if the source matches the conditions
        if (expression.evaluate(source)) {

            // matched the condition, process the suffix path that is pointing to the items to be removed
            if (suffixPath.isEmpty()) {
                parentObject.remove(key);
            } else {
                // locate the final target field to be removed by field name or from path
                int lastPathIndexProp = suffixPath.toString().lastIndexOf('/');
                String propertyName = suffixPath.toString().substring(lastPathIndexProp + 1);
                JsonNode target = parentObject.at(suffixPath.toString().substring(0, lastPathIndexProp));

                // remove the property directly in the object
                if (target.isObject()) {
                    ((ObjectNode) target).remove(propertyName);
                } else if (target.isArray()) {
                    // keep track in case array is not of object but simple types
                    List<Integer> indexesOfTextualFields = new ArrayList<>();
                    AtomicInteger index = new AtomicInteger(0);
                    // or property in every object of the array of objects
                    ((ArrayNode) target).forEach(item -> {
                        if (item.isObject()) {
                            ((ObjectNode) item).remove(propertyName);
                        } else if (item.isTextual() && item.textValue().equals(propertyName)) {
                            indexesOfTextualFields.add(index.get());
                        }
                        index.incrementAndGet();
                    });
                    // in case there were matches by simple type comparison, remove all matched indexes
                    if (!indexesOfTextualFields.isEmpty()) {
                        indexesOfTextualFields.forEach(i -> ((ArrayNode) target).remove(i));
                    }
                }
            }
        }
    }

    private void processArray(JsonNode jsonNode, int lastPathIndexExp, String key) {
        FilterOnExpressionPredicate expressionPredicate = new FilterOnExpressionPredicate(lastPathIndexExp);
        // removing from an array
        ArrayNode parentArray = (ArrayNode) jsonNode;

        if (suffixPath.isEmpty()) {
            // if there is no suffix just remove directly based on expression evaluation

            parentArray.removeIf(item -> expressionPredicate.test(item));
        } else {
            // suffix is there so first find the elements of the collection that match the condition
            List<JsonNode> items = parentArray.valueStream().filter(item -> expressionPredicate.test(item)).toList();
            // for each item that matched the condition perform the removal
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
                            performer = new RemoveByFilter(fragment, suffixPath);

                        } else {
                            // regular path as json pointer so find the parent that expression will be applied on
                            parent = item.at(fragment);
                        }

                    }
                    performer.apply(parent);
                } else {
                    // locate the final target field to be removed by field name or from path
                    int lastPathIndexProp = suffixPath.toString().lastIndexOf('/');
                    String propertyName = suffixPath.toString().substring(lastPathIndexProp + 1);
                    JsonNode target = item.at(suffixPath.toString().substring(0, lastPathIndexProp));
                    // if the final node is an object remove its field
                    if (target.isObject()) {
                        ((ObjectNode) target).remove(propertyName);
                    } else if (target.isArray()) {
                        // if the target node is array
                        try {
                            // attempt to remove by index if property name is an integer/index
                            int index = Integer.parseInt(propertyName);
                            ((ArrayNode) target).remove(index);
                        } catch (NumberFormatException e) {
                            // if property name is not an index (field or path) remove by filter again
                            if (JsonPatchConstants.HAS_EXPRESSION_PATTERN.matcher(propertyName).matches()) {
                                // the property name is an expression so evaluate
                                ExpressionHandler exp = ExpressionHandler.from(propertyName);
                                // remove all elements of the arrach that match the condition
                                ((ArrayNode) target).removeIf(val -> {
                                    return exp.evaluate(val);
                                });
                            } else {
                                // keep track in case array is not of object but simple types
                                List<Integer> indexesOfTextualFields = new ArrayList<>();
                                AtomicInteger index = new AtomicInteger(0);
                                // it is a property name so remove that property from every element of the array
                                ((ArrayNode) target).valueStream().forEach(val -> {
                                    if (val.isObject()) {
                                        ((ObjectNode) val).remove(propertyName);
                                    } else if (val.isTextual() && val.textValue().equals(propertyName)) {
                                        indexesOfTextualFields.add(index.get());
                                    }
                                    index.incrementAndGet();
                                });
                                // in case there were matches by simple type comparison, remove all matched indexes
                                if (!indexesOfTextualFields.isEmpty()) {
                                    indexesOfTextualFields.forEach(i -> ((ArrayNode) target).remove(i));
                                }
                            }
                        } catch (IndexOutOfBoundsException e) {
                            // ignore out of bound indexes
                        }
                    }
                }
            });
        }
    }

    private class FilterOnExpressionPredicate implements Predicate<JsonNode> {

        private int lastPathIndexExp;

        public FilterOnExpressionPredicate(int lastPathIndexExp) {
            this.lastPathIndexExp = lastPathIndexExp;
        }

        @Override
        public boolean test(JsonNode item) {
            // if the field part of the expression was a path, locate the actual object to test on its fields
            if (lastPathIndexExp > 0) {
                item = item.at("/" + expression.getFieldOrPath().substring(0, lastPathIndexExp));
            }
            // if the item is object evaluate on it
            if (item instanceof ObjectNode) {
                return expression.evaluate(item);
            } else if (item instanceof ArrayNode) {
                // if it is an array evaluate each element of the arrach
                return ((ArrayNode) item).valueStream().anyMatch(val -> expression.evaluate(val));
            } else {
                return expression.evaluate(item);
            }
        }

    }
}
